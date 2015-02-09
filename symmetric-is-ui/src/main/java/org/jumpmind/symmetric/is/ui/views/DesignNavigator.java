package org.jumpmind.symmetric.is.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.Icons;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class DesignNavigator extends Panel {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    MenuItem newFolder;
    MenuItem newFlow;
    MenuItem newConnection;
    MenuItem newModel;
    MenuItem delete;
    IConfigurationService configurationService;
    FolderType folderType;
    TreeTable treeTable;
    Set<Object> lastSelected;

    public DesignNavigator(FolderType folderType, IConfigurationService configurationService) {

        this.folderType = folderType;
        this.configurationService = configurationService;

        setCaption("Navigator");
        setSizeFull();
        addStyleName("noborder");
        addStyleName(ValoTheme.MENU_ROOT);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);
        content.addComponent(buildMenuBar());

        treeTable = buildTreeTable();
        content.addComponent(treeTable);
        content.setExpandRatio(treeTable, 1);
    }

    protected HorizontalLayout buildMenuBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);

        newFolder = leftMenuBar.addItem("", Icons.FOLDER_OPEN, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newFolder.setStyleName("folder");
        newFolder.setDescription("New Folder");

        newFlow = leftMenuBar.addItem("", Icons.FLOW, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newFlow.setDescription("New Flow");

        newConnection = leftMenuBar.addItem("", Icons.GENERAL_CONNECTION, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newConnection.setDescription("New Connection");

        newModel = leftMenuBar.addItem("", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        newModel.setDescription("New Model");

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        delete = rightMenuBar.addItem("", Icons.DELETE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
        delete.setDescription("Delete");

        layout.addComponent(leftMenuBar);
        layout.addComponent(rightMenuBar);
        layout.setExpandRatio(leftMenuBar, 1);

        return layout;
    }

    protected TreeTable buildTreeTable() {
        final TreeTable table = new TreeTable();
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setSizeFull();
        table.setImmediate(true);
        table.setMultiSelect(true);
        table.setSelectable(true);
        table.addGeneratedColumn("name", new ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                Label label = new Label(itemId.toString());
                label.addStyleName("leftPad");
                return label;
            }
        });
        table.setColumnExpandRatio("name", 1);
        table.addShortcutListener(new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delete.isEnabled()) {
                    handleDelete();
                }
            }
        });

        table.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                @SuppressWarnings("unchecked")
                Set<Object> selectedIds = (Set<Object>) table.getValue();
                for (Object object : selectedIds) {
                    itemClicked(object);
                }
            }
        });
        table.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(ValueChangeEvent event) {
                lastSelected = (Set<Object>) table.getValue();
                treeSelectionChanged(event);
            }
        });
        table.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (lastSelected != null && lastSelected.contains(event.getItemId())) {
                        table.unselect(event.getItemId());
                    }
                    if (event.isDoubleClick()) {
                        itemClicked(event.getItemId());
                    }
                }
            }
        });
        table.addCollapseListener(new CollapseListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
                }
            }
        });
        table.addExpandListener(new ExpandListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    Folder folder = (Folder) event.getItemId();
                    table.setItemIcon(folder, FontAwesome.FOLDER_OPEN);
                    folderExpanded(folder);
                }
            }
        });
        table.setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (itemId instanceof Folder && "name".equals(propertyId)) {
                    return "folder";
                } else {
                    return null;
                }

            }
        });
        return table;
    }

    protected void itemClicked(Object item) {
    }

    protected Folder getSelectedFolder() {
        @SuppressWarnings("unchecked")
        Set<Object> selectedIds = (Set<Object>) treeTable.getValue();
        for (Object object : selectedIds) {
            if (object instanceof Folder) {
                return (Folder) object;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getSingleSelection(Class<T> clazz) {
        Set<Object> selectedIds = (Set<Object>) treeTable.getValue();
        if (selectedIds != null && selectedIds.size() == 1) {
            Object obj = selectedIds.iterator().next();
            if (obj != null && clazz.isAssignableFrom(obj.getClass())) {
                return (T) obj;
            }
        }
        return null;
    }

    protected void folderExpanded(Folder folder) {

    }

    protected void treeSelectionChanged(ValueChangeEvent event) {
        @SuppressWarnings("unchecked")
        Set<Object> selected = (Set<Object>) treeTable.getValue();
        Folder folder = getSelectedFolder();
        boolean enabled = (folder == null && selected.size() == 0) ||
                 folder != null;
        newFolder.setEnabled(enabled);

        boolean deleteEnabled = false;
        for (Object object : selected) {
            deleteEnabled |= isDeleteButtonEnabled(object);
        }
        delete.setEnabled(deleteEnabled);
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return selected instanceof Folder;
    }

    protected void handleDelete() {
        if (getSelectedFolder() != null) {
            ConfirmDialog.show("Delete Folder?",
                    "Are you sure you want to delete the selected folders?",
                    new IConfirmListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean onOk() {
                            deleteSelectedFolders();
                            refresh();
                            return true;
                        }
                    });
        }

        @SuppressWarnings("unchecked")
        Set<Object> objects = (Set<Object>) treeTable.getValue();
        Collection<Object> noFolders = new HashSet<Object>();
        for (Object object : objects) {
            if (!(object instanceof Folder)) {
                noFolders.add(object);
            }
        }
        if (noFolders.size() > 0) {
            deleteTreeItems(noFolders);
        }
    }

    protected void deleteSelectedFolders() {
        @SuppressWarnings("unchecked")
        Set<Object> selected = (Set<Object>) treeTable.getValue();
        for (Object obj : selected) {
            if (obj instanceof Folder) {
                Folder folder = (Folder) obj;
                try {
                    configurationService.deleteFolder(folder.getData().getId());
                } catch (Exception ex) {
                    CommonUiUtils.notify("Could not delete the \"" + folder.getData().getName()
                            + "\" folder", Type.WARNING_MESSAGE);
                }
            }
        }
    }

    public void refresh() {
        @SuppressWarnings({ "unchecked" })
        Set<Object> selected = (Set<Object>) treeTable.getValue();
        List<Object> expandedItems = new ArrayList<Object>();
        Collection<?> items = treeTable.getItemIds();
        for (Object object : items) {
            if (!treeTable.isCollapsed(object)) {
                expandedItems.add(object);
            }
        }

        this.treeTable.removeAllItems();
        List<Folder> folders = configurationService.findFolders(folderType);
        for (Folder folder : folders) {
            addChildFolder(folder);
        }

        for (Object object : expandedItems) {
            treeTable.setCollapsed(object, false);
        }

        treeTable.focus();
        if (treeTable.containsId(selected)) {
            treeTable.setValue(selected);
        }

    }

    protected void addChildFolder(Folder folder) {
        this.treeTable.addItem(folder);
        this.treeTable.setItemIcon(folder, FontAwesome.FOLDER);
        this.treeTable.setCollapsed(folder, true);
        if (folder.getParent() != null) {
            this.treeTable.setParent(folder, folder.getParent());
        }
        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildFolder(child);
        }
    }

    protected void deleteTreeItems(Collection<Object> objects) {

    }

}