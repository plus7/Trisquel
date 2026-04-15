package net.tnose.app.trisquel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CheckListDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun checkListDialog_handlesAsyncItemLoadingWithoutCrashing() {
        // テスト用の状態（最初は空リスト）
        var items by mutableStateOf(emptyList<String>())
        var initialCheckedIndices by mutableStateOf(emptyList<Int>())

        composeTestRule.setContent {
            CheckListDialog(
                title = "テストダイアログ",
                items = items,
                initialCheckedIndices = initialCheckedIndices,
                onConfirm = {},
                onDismiss = {}
            )
        }

        // 1. 最初は空リストで描画（ここでクラッシュしないことを確認）
        composeTestRule.waitForIdle()

        // 2. 非同期でデータが読み込まれた状況をシミュレート（リストを更新）
        items = listOf("アクセサリA", "アクセサリB")
        initialCheckedIndices = listOf(0)

        // 3. 再コンポーズを待機
        composeTestRule.waitForIdle()

        // 4. 新しく追加された要素がクラッシュせずに正しく表示されているか検証
        // （修正前はここでIndexOutOfBoundsExceptionが発生してテストが失敗します）
        composeTestRule.onNodeWithText("アクセサリA").assertIsDisplayed()
        composeTestRule.onNodeWithText("アクセサリB").assertIsDisplayed()
        
        // 5. チェックボックスをクリックしてもクラッシュしないか検証
        composeTestRule.onNodeWithText("アクセサリB").performClick()
        composeTestRule.waitForIdle()
    }
}
