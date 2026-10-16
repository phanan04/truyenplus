package ireader.presentation.ui.settings.category

import ireader.presentation.ui.truyenplus.ReaderGlassButton as Button
import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedButton as OutlinedButton
import ireader.presentation.ui.truyenplus.ReaderGlassTextButton as TextButton
import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton
import ireader.presentation.ui.truyenplus.ReaderGlassFilterChip as FilterChip
import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedTextField as OutlinedTextField

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import ireader.presentation.ui.truyenplus.LocalReaderFullGlass
import ireader.presentation.ui.truyenplus.readerOptionalGlass
import ireader.presentation.ui.truyenplus.readerOptionalGlassColor
import ireader.presentation.ui.truyenplus.ReaderGlassSnackbarHost as SnackbarHost
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore

import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Source


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import ireader.presentation.ui.truyenplus.readerAppGlassCard
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import ireader.presentation.ui.truyenplus.readerPanel
import ireader.presentation.ui.truyenplus.ReaderGlassDialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.entities.CategoryAutoRule
import ireader.domain.models.entities.CategoryWithCount
import ireader.domain.utils.extensions.launchIO
import ireader.i18n.localize
import ireader.i18n.resources.Res
import ireader.i18n.resources.*
import ireader.presentation.ui.component.components.IAlertDialog
import ireader.presentation.ui.component.components.PreferenceRow
import ireader.presentation.ui.component.reorderable.*
import ireader.presentation.ui.component.reusable_composable.AppIconButton
import ireader.presentation.ui.component.reusable_composable.AppTextField
import ireader.presentation.ui.component.reusable_composable.MidSizeTextComposable
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.settings.components.SettingsConfirmationDialog
import ireader.presentation.ui.settings.components.SettingsTextInputDialog
import ireader.presentation.ui.settings.components.SettingsSwitchItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    vm: CategoryScreenViewModel,
) {
    // Collect categories as state - this will automatically update when the flow emits
    val data by vm.categories.collectAsState()

    val localizeHelper = requireNotNull(LocalLocalizeHelper.current) { "LocalLocalizeHelper not provided" }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var categoryToDelete by remember { mutableStateOf<CategoryWithCount?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var categoryToRename by remember { mutableStateOf<CategoryWithCount?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val state: ReorderableLazyListState = rememberReorderLazyListState(
        onMove = { from, to ->
            scope.launchIO {
                vm.reorderCategory.await(data[from.index].id, newPosition = to.index)
            }
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Show Empty Categories Toggle using unified component
            SettingsSwitchItem(
                title = localizeHelper.localize(Res.string.show_empty_categories),
                description = localizeHelper.localize(Res.string.tp_category_empty_hint),
                checked = vm.showEmptyCategories.value,
                onCheckedChange = { vm.showEmptyCategories.value = it },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            CategoryContent(
                state = state,
                data = data,
                vm = vm,
                onDelete = { category ->
                    categoryToDelete = category
                    showDeleteConfirmation = true
                },
                onRename = { category ->
                    categoryToRename = category
                    showRenameDialog = true
                }
            )
        }

        CategoryFloatingActionButton(vm)

        // Snackbar host for undo functionality
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Create category dialog using unified component
    if (vm.showDialog) {
        SettingsTextInputDialog(
            title = localize(Res.string.edit_category),
            label = localize(Res.string.category_hint),
            placeholder = localizeHelper.localize(Res.string.tp_category_example),
            confirmText = localize(Res.string.confirm),
            validator = { text ->
                when {
                    text.isEmpty() -> localizeHelper.localize(Res.string.tp_category_name_empty)
                    text.length < 2 -> localizeHelper.localize(Res.string.tp_category_name_short)
                    data.any { it.name.equals(text, ignoreCase = true) } ->
                        localizeHelper.localize(Res.string.tp_category_name_exists)
                    else -> null
                }
            },
            onConfirm = { text ->
                vm.createCategory(text)
                vm.showDialog = false
            },
            onDismiss = {
                vm.showDialog = false
            }
        )
    }

    // Rename dialog using unified component
    if (showRenameDialog && categoryToRename != null) {
        SettingsTextInputDialog(
            title = localizeHelper.localize(Res.string.rename_category),
            initialValue = categoryToRename!!.name,
            label = localizeHelper.localize(Res.string.category_name),
            placeholder = localizeHelper.localize(Res.string.tp_category_new_name),
            confirmText = localize(Res.string.rename_category),
            icon = Icons.Default.Edit,
            validator = { newName ->
                when {
                    newName.isEmpty() -> localizeHelper.localize(Res.string.tp_category_name_empty)
                    newName.length < 2 -> localizeHelper.localize(Res.string.tp_category_name_short)
                    newName == categoryToRename!!.name -> "Please enter a different name"
                    data.any { it.name.equals(newName, ignoreCase = true) } ->
                        localizeHelper.localize(Res.string.tp_category_name_exists)
                    else -> null
                }
            },
            onConfirm = { newName ->
                vm.scope.launch {
                    vm.renameCategory(categoryToRename!!.id, newName)
                }
                showRenameDialog = false
                categoryToRename = null
            },
            onDismiss = {
                showRenameDialog = false
                categoryToRename = null
            }
        )
    }

    // Delete confirmation dialog using unified component
    if (showDeleteConfirmation && categoryToDelete != null) {
        val category = categoryToDelete!!
        val message=localize(Res.string.tp_category_delete_question,category.name)+"\n\n"+localize(Res.string.tp_category_delete_hint)
        val deletedMessage=localize(Res.string.tp_category_deleted,category.name)
        val undoLabel=localize(Res.string.undo)
        val failureMessage=localize(Res.string.tp_operation_failed)
        SettingsConfirmationDialog(
            title = localizeHelper.localize(Res.string.delete_category),
            message = message,
            confirmText = localize(Res.string.delete_category),
            icon = Icons.Default.DeleteForever,
            isDestructive = true,
            onConfirm = {
                val deletedCategory = categoryToDelete!!
                vm.scope.launch {
                    try {
                        val snapshot=vm.categoriesUseCase.deleteCategoryWithUndo(deletedCategory.category)
                        val result=snackbarHostState.showSnackbar(deletedMessage,actionLabel=undoLabel,duration=SnackbarDuration.Long)
                        if(result==SnackbarResult.ActionPerformed)vm.categoriesUseCase.restoreDeletedCategory(snapshot)
                    }catch(e:kotlinx.coroutines.CancellationException){throw e}
                    catch(_:Exception){snackbarHostState.showSnackbar(failureMessage)}
                }
                showDeleteConfirmation = false
                categoryToDelete = null
            },
            onDismiss = {
                showDeleteConfirmation = false
                categoryToDelete = null
            }
        )
    }

    // Auto-categorization rules dialog
    if (vm.showAutoRulesDialog && vm.selectedCategoryForRules != null) {
        AutoCategorizationRulesDialog(
            category = vm.selectedCategoryForRules!!,
            rules = vm.getRulesForCategory(vm.selectedCategoryForRules!!.id),
            onAddGenreRule = { genre ->
                vm.addGenreRule(vm.selectedCategoryForRules!!.id, genre)
            },
            onAddSourceRule = { sourceId, sourceName ->
                vm.addSourceRule(vm.selectedCategoryForRules!!.id, sourceId, sourceName)
            },
            onToggleRule = { ruleId, enabled -> vm.toggleRule(ruleId, enabled) },
            onDeleteRule = { ruleId -> vm.deleteRule(ruleId) },
            onDismiss = {
                vm.showAutoRulesDialog = false
                vm.selectedCategoryForRules = null
            }
        )
    }
}

@Composable
fun CategoryFloatingActionButton(
    vm: CategoryScreenViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {

        androidx.compose.material3.ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp).readerOptionalGlass(32),
            text = {
                MidSizeTextComposable(
                    text = localize(Res.string.add),
                    color = if(LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary
                )
            },
            onClick = {
                vm.showDialog = true
            },
            icon = {
                Icon(Icons.Filled.Add, "", tint = if(LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary)
            },
            contentColor = if(LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary,
            containerColor = readerOptionalGlassColor(MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
private fun CategoryContent(
    state: ReorderableLazyListState,
    data: List<CategoryWithCount>,
    vm: CategoryScreenViewModel,
    onDelete: (CategoryWithCount) -> Unit,
    onRename: (CategoryWithCount) -> Unit
) {
    // "All" is shown and reorderable; Uncategorized stays hidden
    val userCategories = remember(data) {
        data.filter { it.id != ireader.domain.models.entities.Category.UNCATEGORIZED_ID }
    }

    LazyColumn(
        state = state.listState,
        modifier = Modifier.reorderable(state)
    ) {
        items(
            items = userCategories,
            key = {
                it.id
            }
        ) { item ->
            val index=userCategories.indexOfFirst {it.id==item.id}
            val rules = vm.getRulesForCategory(item.id)
            EnhancedCategoryItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggedItem(state.offsetByKey(item.id))
                    .detectReorderAfterLongPress(state),
                category = item,
                rules = rules,
                onDelete = { onDelete(item) },
                onRename = { onRename(item) },
                onConfigureAutoRules = {
                    vm.selectedCategoryForRules = item
                    vm.showAutoRulesDialog = true
                },
                onToggleRule = { ruleId, enabled -> vm.toggleRule(ruleId, enabled) },
                onDeleteRule = { ruleId -> vm.deleteRule(ruleId) },
                onMoveUp=if(index>0){{vm.scope.launch {vm.reorderCategory.await(item.id,index-1)}}} else null,
                onMoveDown=if(index<userCategories.lastIndex){{vm.scope.launch {vm.reorderCategory.await(item.id,index+1)}}} else null
            )
        }
    }
}

@Composable
internal fun EnhancedCategoryItem(
    modifier:Modifier=Modifier,category:CategoryWithCount,rules:List<CategoryAutoRule>,onDelete:()->Unit,onRename:()->Unit,
    onConfigureAutoRules:()->Unit,onToggleRule:(Long,Boolean)->Unit,onDeleteRule:(Long)->Unit,
    onMoveUp:(()->Unit)?=null,onMoveDown:(()->Unit)?=null
) {
    var menu by remember {mutableStateOf(false)}
    var expanded by remember {mutableStateOf(false)}
    val up=localize(Res.string.tp_category_move_up)
    val down=localize(Res.string.tp_category_move_down)
    Surface(modifier.padding(horizontal=12.dp,vertical=4.dp).readerAppGlassCard(20).semantics {
        customActions=listOfNotNull(onMoveUp?.let {CustomAccessibilityAction(up){it();true}},onMoveDown?.let {CustomAccessibilityAction(down){it();true}})
    },shape=RoundedCornerShape(20.dp),color=androidx.compose.ui.graphics.Color.Transparent) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.DragHandle,localize(Res.string.drag_to_reorder),Modifier.width(24.dp).padding(end=8.dp))
                Column(Modifier.weight(1f)) {
                    Text(if(category.id==0L)localize(Res.string.tp_all) else category.name,style=MaterialTheme.typography.titleMedium,maxLines=3,overflow=TextOverflow.Ellipsis,
                        modifier=Modifier.testTag("category-name"))
                    Text(localize(Res.string.tp_category_book_count,category.bookCount),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(!category.isSystemCategory)Box {
                    IconButton({menu=true},Modifier.size(48.dp).testTag("category-actions")){Icon(Icons.Default.MoreVert,localize(Res.string.more))}
                    DropdownMenu(menu,{menu=false},modifier=Modifier.readerPanel(20),containerColor=androidx.compose.ui.graphics.Color.Transparent) {
                        DropdownMenuItem(text={Text(localize(Res.string.rename_category))},onClick={menu=false;onRename()})
                        DropdownMenuItem(text={Text(localize(Res.string.tp_category_auto_rules))},onClick={menu=false;onConfigureAutoRules()})
                        DropdownMenuItem(text={Text(localize(Res.string.delete_category))},onClick={menu=false;onDelete()})
                    }
                }
            }
            if(rules.isNotEmpty())TextButton({expanded=!expanded},Modifier.heightIn(min=48.dp)) {
                Text(localize(Res.string.tp_category_rule_count,rules.size))
                Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
            }
            AnimatedVisibility(expanded && rules.isNotEmpty()) {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    rules.forEach {rule->AutoRuleChip(rule,{onToggleRule(rule.id,!rule.isEnabled)},{onDeleteRule(rule.id)})}
                }
            }
        }
    }
}

@Composable
private fun AutoRuleChip(
    rule: CategoryAutoRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 2.dp).readerOptionalGlass(8)
            .clickable { onToggle() },
        shape = RoundedCornerShape(8.dp),
        color = if(LocalReaderFullGlass.current)androidx.compose.ui.graphics.Color.Transparent else if (rule.isEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (rule.ruleType) {
                    CategoryAutoRule.RuleType.GENRE -> Icons.AutoMirrored.Filled.Label
                    CategoryAutoRule.RuleType.SOURCE -> Icons.Default.Source
                },

                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (rule.isEnabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = localize(if(rule.ruleType==CategoryAutoRule.RuleType.GENRE)Res.string.tp_category_genre else Res.string.tp_category_source)+": "+rule.value,
                style = MaterialTheme.typography.bodySmall,
                color = if(LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface else if (rule.isEnabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = localize(Res.string.tp_edit_delete),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCategorizationRulesDialog(
    category: CategoryWithCount,
    rules: List<CategoryAutoRule>,
    onAddGenreRule: (String) -> Unit,
    onAddSourceRule: (Long, String?) -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddRuleSection by remember { mutableStateOf(false) }
    var selectedRuleType by remember { mutableStateOf(CategoryAutoRule.RuleType.GENRE) }
    var ruleValue by remember { mutableStateOf("") }

    ReaderGlassDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(localize(Res.string.tp_category_rules_title,category.name))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max=ireader.presentation.ui.truyenplus.LocalReaderViewport.current.height*.58f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = localize(Res.string.tp_category_rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Existing rules
                if (rules.isNotEmpty()) {
                    Text(
                        text = localize(Res.string.tp_category_rules_current),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    rules.forEach { rule ->
                        AutoRuleItem(
                            rule = rule,
                            onToggle = { onToggleRule(rule.id, !rule.isEnabled) },
                            onDelete = { onDeleteRule(rule.id) }
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().readerOptionalGlass(8),
                        shape = RoundedCornerShape(8.dp),
                        color = readerOptionalGlassColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = localize(Res.string.tp_category_rules_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Add new rule section
                AnimatedVisibility(visible = showAddRuleSection) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = localize(Res.string.tp_category_add_rule),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Rule type selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRuleType == CategoryAutoRule.RuleType.GENRE,
                                onClick = { selectedRuleType = CategoryAutoRule.RuleType.GENRE },
                                label = { Text(localize(Res.string.tp_category_genre)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Label,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )

                                }
                            )
                            FilterChip(
                                selected = selectedRuleType == CategoryAutoRule.RuleType.SOURCE,
                                onClick = { selectedRuleType = CategoryAutoRule.RuleType.SOURCE },
                                label = { Text(localize(Res.string.tp_category_source)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Source,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        // Value input
                        OutlinedTextField(
                            value = ruleValue,
                            onValueChange = { ruleValue = it },
                            label = {
                                Text(
                                    if (selectedRuleType == CategoryAutoRule.RuleType.GENRE)
                                        localize(Res.string.tp_category_genre_name)
                                    else
                                        localize(Res.string.tp_category_source_name)
                                )
                            },
                            placeholder = {
                                Text(
                                    if (selectedRuleType == CategoryAutoRule.RuleType.GENRE)
                                        localize(Res.string.tp_category_genre_example)
                                    else
                                        localize(Res.string.tp_category_source_example)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showAddRuleSection = false
                                ruleValue = ""
                            }) {
                                Text(localize(Res.string.cancel))
                            }

                            Button(
                                onClick = {
                                    if (ruleValue.isNotBlank()) {
                                        when (selectedRuleType) {
                                            CategoryAutoRule.RuleType.GENRE -> {
                                                onAddGenreRule(ruleValue.trim())
                                            }
                                            CategoryAutoRule.RuleType.SOURCE -> {
                                                val sourceId = ruleValue.trim().toLongOrNull()
                                                if (sourceId != null) {
                                                    onAddSourceRule(sourceId, null)
                                                } else {
                                                    // Treat as source name - use hash as ID
                                                    onAddSourceRule(ruleValue.hashCode().toLong(), ruleValue.trim())
                                                }
                                            }
                                        }
                                        ruleValue = ""
                                        showAddRuleSection = false
                                    }
                                },
                                enabled = ruleValue.isNotBlank()
                            ) {
                                Text(localize(Res.string.tp_category_add_rule))
                            }
                        }
                    }
                }

                // Add rule button
                if (!showAddRuleSection) {
                    OutlinedButton(
                        onClick = { showAddRuleSection = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localize(Res.string.tp_category_add_rule))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(localize(Res.string.done))
            }
        }
    )
}

@Composable
private fun AutoRuleItem(
    rule: CategoryAutoRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().readerOptionalGlass(8),
        shape = RoundedCornerShape(8.dp),
        color = if(LocalReaderFullGlass.current)androidx.compose.ui.graphics.Color.Transparent else if (rule.isEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (rule.ruleType) {
                    CategoryAutoRule.RuleType.GENRE -> Icons.AutoMirrored.Filled.Label
                    CategoryAutoRule.RuleType.SOURCE -> Icons.Default.Source
                },

                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (rule.isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if(LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface else if (rule.isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = rule.ruleType.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.height(24.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = localize(Res.string.tp_edit_delete),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
