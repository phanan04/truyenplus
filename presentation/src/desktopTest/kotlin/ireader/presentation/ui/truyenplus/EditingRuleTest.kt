package ireader.presentation.ui.truyenplus
import ireader.domain.models.editing.EditingRule
import kotlin.test.*
class EditingRuleTest {
    @Test fun literalRulesKeepPunctuationAndDollarSignsAndWordBoundariesHandleVietnamese() {
        val rule=EditingRule("Yên Trần.","Yên Trần $100",wholeWord=true)
        val regex=Regex(rule.pattern())
        assertEquals("Yên Trần $100",regex.replace("Yên Trần.",rule.replacement()))
        assertEquals("TênYên Trần.",regex.replace("TênYên Trần.",rule.replacement()))
        assertEquals("Yên TrầnX",regex.replace("Yên TrầnX",rule.replacement()))
    }
}
