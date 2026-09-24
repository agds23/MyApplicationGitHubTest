package com.example.myapplication_githubtest.call

import android.telecom.Call
import android.telecom.CallScreeningService

class FoneGuardCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        // Baseline behavior: FoneGuard observes the screening callback but does not
        // restrict the call. Policy evaluation will be introduced in later increments.
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}
