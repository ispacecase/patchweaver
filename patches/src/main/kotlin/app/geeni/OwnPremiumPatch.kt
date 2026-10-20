package app.geeni

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

// com.merkury.geeni.morphe 3.21.0 (versionCode 32100038), mk.apk — "Merkury Smart" (MERKURY
// white-label of Pepper Robotics, hence every package is com.pepper.*).
private val COMPATIBILITY_MERKURY = Compatibility(
    name = "Merkury Smart",
    packageName = "com.merkury.geeni.morphe",
    targets = listOf(
        AppTarget(version = "3.21.0"),
    ),
)

/**
 * Every piece of premium/subscription state this app consumes crosses the React Native bridge
 * through exactly one class: `com.pepper.reactnativepeppersdk.PepperSdkModule` (classes7.dex), a
 * Kotlin/RN module whose four plan/subscription entry points (`listSubscriptions`,
 * `listAvailablePlans`, `getPlansByBrand`, `listSubscriptionsByAccount`) are the only ones the
 * Hermes bundle (`assets/index.android.bundle`) ever calls for subscription data — verified
 * against the bundle's string pool and the decompiled source. All gating ("premium upsell",
 * trial, plan cards) is pure JS on top of these payloads, so faking them here makes the account
 * read as an active premium owner everywhere at once.
 *
 * Each entry point normally hands the native SDK result to `resolvePromise(promise, output, name)`,
 * which serializes the model with Moshi and calls `promise.resolve(json)` — the JS side therefore
 * receives a JSON string. The fake bodies keep that exact contract: they just resolve a fixed
 * JSON string and return, so no new classes are needed and no extensions are bundled.
 *
 * JSON shapes/values are taken verbatim from the SDK models (`PepperSubscription`,
 * `PepperPlansResponse`, `PepperPlan`, `PepperPlanGroup`, `PepperListSubscriptionsByAccountResponse`)
 * in classes7.dex: kotlinx-style `@Json(name = ...)` keys, enums serialized by entry name via the
 * plain Moshi EnumJsonAdapter (there is no custom enum adapter — see
 * `com.pepper.peppersdk.infrastructure.Serializer`) → `status "ACTIVE"`, `provider "recurly"`,
 * trial unit `"day"`, currency `"uSD"` (the entry is literally named `uSD`). The subscription is
 * dated far in the future so renewal/trial expiry comparisons keep passing on every load.
 */
// Single lines (no surrounding whitespace) so they embed verbatim into `const-string` smali.
private const val SUBSCRIPTION_JSON = """[{\"device_ids\":[],\"external_subscription_id\":\"sub_premium_owned\",\"id\":\"sub_premium_owned\",\"paused\":null,\"pending_changes\":null,\"plan_code\":\"year_premium\",\"provider\":\"recurly\",\"renewal_date\":\"2099-01-01T00:00:00Z\",\"status\":\"ACTIVE\",\"trial_end_date\":null}]"""

// `PepperPlansResponse` shape: {"plans":[PepperPlan]}.
private const val PLAN_JSON = """{\"billing_cycle\":365,\"billing_order\":2,\"brand\":\"merkury\",\"cost\":[{\"amount\":0,\"currency\":\"uSD\"}],\"default\":true,\"device_quantity\":null,\"external_plan_code\":\"year_premium\",\"features\":{\"activityViewable\":\"31\",\"facialRecognition\":true,\"retention\":31,\"sendHelp\":true,\"smartAlerts\":true,\"snoozeNotifications\":true,\"subscribable\":true},\"id\":\"plan_year_premium\",\"name\":\"merkury_premium\",\"plan_group_code\":\"merkury_premium\",\"provider\":\"recurly\",\"trial\":null,\"visible\":true}"""

// `listAvailablePlans` resolves a List<PepperPlanGroup>: [{"_id":{id,name},"name":..., "plans":[...]}].
private const val AVAILABLE_PLANS_JSON = """[{\"_id\":{\"id\":\"group_premium\",\"name\":\"merkury_premium\"},\"name\":\"merkury_premium\",\"plans\":[${PLAN_JSON}]}]"""

// `getPlansByBrand` resolves a PepperPlansResponse.
private const val PLANS_BY_BRAND_JSON = """{\"plans\":[${PLAN_JSON}]}"""

// `listSubscriptionsByAccount` resolves {"subscriptions":[...], "trialEligibility":{plans:[...]}};
// an owned premium account has nothing left trial-eligible.
private const val SUBSCRIPTIONS_BY_ACCOUNT_JSON = """{\"subscriptions\":${SUBSCRIPTION_JSON},\"trialEligibility\":{\"plans\":[]}}"""

// `listSubscriptions:(Promise)V` — registers=4: v0-v1 locals, v2=this, v3=promise.
private val listSubscriptions = Fingerprint(
    definingClass = "Lcom/pepper/reactnativepeppersdk/PepperSdkModule;",
    name = "listSubscriptions",
    returnType = "V",
    parameters = listOf("Lcom/facebook/react/bridge/Promise;"),
)

// (String, Promise) — registers=5: v0-v1 locals, v2=this, v3=String, v4=promise.
private val listAvailablePlans = Fingerprint(
    definingClass = "Lcom/pepper/reactnativepeppersdk/PepperSdkModule;",
    name = "listAvailablePlans",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lcom/facebook/react/bridge/Promise;"),
)

private val getPlansByBrand = Fingerprint(
    definingClass = "Lcom/pepper/reactnativepeppersdk/PepperSdkModule;",
    name = "getPlansByBrand",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lcom/facebook/react/bridge/Promise;"),
)

private val listSubscriptionsByAccount = Fingerprint(
    definingClass = "Lcom/pepper/reactnativepeppersdk/PepperSdkModule;",
    name = "listSubscriptionsByAccount",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lcom/facebook/react/bridge/Promise;"),
)

val ownPremiumPatch = bytecodePatch(
    name = "Own premium plan",
    description = "Reports the account as an active premium subscription with a fully featured, " +
        "zero-cost premium plan in every plan catalog, so no premium upsell/trial UI appears.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MERKURY)

    execute {
        listSubscriptions.method.addInstructions(
            0,
            """
                const-string v0, "$SUBSCRIPTION_JSON"
                invoke-interface {v3, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
        listAvailablePlans.method.addInstructions(
            0,
            """
                const-string v0, "$AVAILABLE_PLANS_JSON"
                invoke-interface {v4, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
        getPlansByBrand.method.addInstructions(
            0,
            """
                const-string v0, "$PLANS_BY_BRAND_JSON"
                invoke-interface {v4, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
        listSubscriptionsByAccount.method.addInstructions(
            0,
            """
                const-string v0, "$SUBSCRIPTIONS_BY_ACCOUNT_JSON"
                invoke-interface {v4, v0}, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
            """,
        )
    }
}
