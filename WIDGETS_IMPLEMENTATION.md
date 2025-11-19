# Widget Hosting Implementation

This document explains the Android widget hosting implementation for your launcher app.

## Overview

The widget hosting system allows users to add Android home screen widgets to your launcher. When users tap the "Add Widget" button in the WidgetHost component, they can select from available system widgets.

## Architecture

### Components Created

1. **LauncherAppWidgetHost** (`util/LauncherAppWidgetHost.kt`)
   - Custom `AppWidgetHost` that manages widget lifecycle
   - Custom `AppWidgetHostView` for displaying widgets without default padding
   - Uses host ID 1024 for widget identification

2. **WidgetInfo** (`model/WidgetInfo.kt`)
   - Data class representing widget metadata
   - Stores: widgetId, providerName, dimensions, label

3. **WidgetDataSource** (`datastore/WidgetDataSource.kt`)
   - DataStore-based persistence for widget information
   - Supports up to 10 widgets
   - Handles widget addition, removal, and retrieval

4. **WidgetRepository** (`repository/WidgetRepository.kt`)
   - Singleton that manages widget operations
   - Provides access to AppWidgetManager and AppWidgetHost
   - Handles widget lifecycle (allocation, binding, deletion)
   - Maintains StateFlow of widgets for UI observation

5. **WidgetViewModel** (`viewmodel/WidgetViewModel.kt`)
   - Hilt ViewModel for managing widget state in Compose
   - Handles widget selection events
   - Exposes SharedFlows for widget picker and bind events
   - Observes widget list from repository

6. **WidgetHost** (`ui/feature/widgets/WidgetHost.kt`)
   - Composable component that displays widgets
   - Shows "Add Widget" button when tapped
   - Uses AndroidView to embed native widget views
   - Supports long-press to remove widgets
   - Full width, adapts to widget-requested height

7. **MainActivity Integration**
   - Handles widget picker and configuration flows
   - Uses ActivityResultLauncher for widget selection
   - Manages widget binding permissions
   - Listens to ViewModel events for launching system dialogs

## User Flow

### Adding a Widget

1. User taps "Tap to add a widget" button in WidgetHost
2. WidgetViewModel allocates a new widget ID
3. MainActivity receives event and launches system widget picker
4. User selects a widget from the picker
5. If widget requires configuration:
   - Configuration activity is launched
   - User configures widget
6. If widget requires binding permission:
   - System permission dialog is shown
   - User grants permission
7. Widget is saved to DataStore and displayed in WidgetHost

### Removing a Widget

1. User long-presses on a widget
2. Remove button (X) appears as overlay
3. User taps the remove button
4. Widget is deleted from host and removed from DataStore
5. UI updates to remove the widget

## Key Features

- **Full Width**: Widgets take full width of the container
- **Dynamic Height**: Widgets adapt to their requested height
- **Persistent Storage**: Widget configuration persists across app restarts
- **Multiple Widgets**: Support for up to 10 widgets simultaneously
- **Easy Removal**: Long-press to remove with confirmation overlay
- **Configuration Support**: Handles widgets that require setup
- **Permission Handling**: Requests binding permission when needed

## Integration

The WidgetHost is integrated into the home screen through:

1. Added `ListItem.WidgetHost` to the sealed class
2. Added to `buildFavoriteListItems()` in `HomeViewModel`
3. Displayed in `FavoritesView` between MediaWidget and app favorites

## Permissions

Added to AndroidManifest:
```xml
<uses-permission android:name="android.permission.BIND_APPWIDGET" />
```

## Lifecycle Management

- `AppWidgetHost.startListening()` called when repository is initialized
- `AppWidgetHost.stopListening()` called when WidgetViewModel is cleared
- Widget views are created and managed by AndroidView composition
- Widgets automatically receive updates from their providers

## Customization

To customize widget appearance or behavior:

1. **Modify padding**: Update `LauncherAppWidgetHostView.getDefaultPaddingForWidget()`
2. **Change widget limit**: Update `WidgetDataSource.MAX_WIDGETS`
3. **Adjust layout**: Modify `WidgetHost.kt` composable styling
4. **Change host ID**: Update `LauncherAppWidgetHost.WIDGET_HOST_ID`

## Technical Notes

- Uses Jetpack Compose for UI with AndroidView for native widget embedding
- Hilt dependency injection for repository and ViewModel
- Kotlin Coroutines and Flow for asynchronous operations
- DataStore Preferences for persistence
- Activity Result API for widget picker and configuration

## Future Enhancements

Potential improvements:
- Widget resizing support
- Drag-and-drop reordering
- Widget preview before adding
- Per-widget settings (refresh rate, etc.)
- Widget categories/filtering in picker
- Backup and restore widget configuration

## Troubleshooting

**Widget not displaying:**
- Ensure widget provider is still installed
- Check widget permissions in app settings
- Verify widget ID is valid

**Widget picker not opening:**
- Check MainActivity ActivityResultLauncher registration
- Verify BIND_APPWIDGET permission in manifest
- Check logs for Intent resolution errors

**Widgets disappear on restart:**
- Verify DataStore is persisting correctly
- Check WidgetRepository.loadWidgets() is called on init
- Ensure widget IDs are not being reused

## Code Structure

```
app/src/main/java/net/wshmkr/launcher/
├── datastore/
│   └── WidgetDataSource.kt          # Widget persistence
├── model/
│   ├── ListItem.kt                  # Added WidgetHost item
│   └── WidgetInfo.kt                # Widget data model
├── repository/
│   └── WidgetRepository.kt          # Widget management
├── ui/feature/
│   ├── home/
│   │   └── FavoritesView.kt         # Displays WidgetHost
│   └── widgets/
│       └── WidgetHost.kt            # Widget UI component
├── util/
│   └── LauncherAppWidgetHost.kt     # AppWidgetHost wrapper
├── viewmodel/
│   ├── HomeViewModel.kt             # Updated for WidgetHost item
│   └── WidgetViewModel.kt           # Widget state management
└── MainActivity.kt                  # Widget picker integration
```

