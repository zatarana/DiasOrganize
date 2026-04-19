package com.lifeflow.pro.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lifeflow.pro.R

private data class OnboardingPage(val titleRes: Int, val bodyRes: Int)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_body_1),
        OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_body_2),
        OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_body_3),
    )
    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]
    val isLastPage = currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(page.titleRes), style = MaterialTheme.typography.headlineSmall)
                Text(text = stringResource(page.bodyRes), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.onboarding_step_indicator, currentPage + 1, pages.size),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Button(
            onClick = {
                if (isLastPage) onFinish() else currentPage += 1
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    if (isLastPage) R.string.action_start else R.string.action_next,
                ),
            )
        }
    }
}
