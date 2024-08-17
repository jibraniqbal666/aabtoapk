import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CollapsableTextField(text: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isExpanded) "Hide Details" else "Show Details",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { isExpanded = !isExpanded },
            color = SecondaryTextColor
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                TextField(
                    value = text,
                    onValueChange = {},
                    textStyle = MaterialTheme.typography.caption,
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = PrimaryTextColor,
                        backgroundColor = BackgroundColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(width = 1.dp, color = SecondaryTextColor, shape = RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
