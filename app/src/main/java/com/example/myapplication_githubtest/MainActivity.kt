package com.example.myapplication_githubtest

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication_githubtest.ui.theme.MyApplicationGitHubTestTheme

class MainActivity : ComponentActivity() {
    private var isCallScreeningRoleSupported by mutableStateOf(false)
    private var isCallScreeningRoleHeld by mutableStateOf(false)

    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshCallScreeningRoleState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshCallScreeningRoleState()

        setContent {
            MyApplicationGitHubTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FoneGuardScreen(
                        isRoleSupported = isCallScreeningRoleSupported,
                        isRoleHeld = isCallScreeningRoleHeld,
                        onRequestRole = ::requestCallScreeningRole,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCallScreeningRoleState()
    }

    private fun refreshCallScreeningRoleState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            isCallScreeningRoleSupported = false
            isCallScreeningRoleHeld = false
            return
        }

        val roleManager = getSystemService(RoleManager::class.java)
        isCallScreeningRoleSupported = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
        isCallScreeningRoleHeld = isCallScreeningRoleSupported &&
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val roleManager = getSystemService(RoleManager::class.java)
        if (
            roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            callScreeningRoleLauncher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            )
        }
    }
}

@Composable
private fun FoneGuardScreen(
    isRoleSupported: Boolean,
    isRoleHeld: Boolean,
    onRequestRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.call_screening_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(text = stringResource(R.string.call_screening_description))

        val statusText = when {
            !isRoleSupported -> stringResource(R.string.call_screening_status_unsupported)
            isRoleHeld -> stringResource(R.string.call_screening_status_active)
            else -> stringResource(R.string.call_screening_status_inactive)
        }
        Text(text = statusText, style = MaterialTheme.typography.titleMedium)

        if (isRoleSupported && !isRoleHeld) {
            Button(onClick = onRequestRole) {
                Text(text = stringResource(R.string.call_screening_request_role))
            }
        }

        if (isRoleHeld) {
            Text(text = stringResource(R.string.call_screening_allow_all_notice))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoneGuardScreenPreview() {
    MyApplicationGitHubTestTheme {
        FoneGuardScreen(
            isRoleSupported = true,
            isRoleHeld = false,
            onRequestRole = {}
        )
    }
}
