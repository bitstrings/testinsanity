package org.bitstrings.idea.plugins.testinsanity;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.bitstrings.idea.plugins.testinsanity.config.ProjectConfigParser;
import org.bitstrings.idea.plugins.testinsanity.config.TestInsanityConfig;

import com.intellij.openapi.options.ex.ConfigurableCardPanel;
import com.intellij.util.ui.JBUI;

public class TestInsanityFormTest
    extends TestInsanityFixtureTestCase
{
    private static final int CRAMPED_SETTINGS_PANE_WIDTH = 120;

    private static final int SETTINGS_PANE_HEIGHT = 600;

    private static final String LONG_SCHEME_NAME = "integration tests of the billing subsystem";

    public void testCreateComponent_crampedSettingsPane_needsNoHorizontalScrollBar()
    {
        JScrollPane settingsPane = settingsPane();

        boolean scrollsHorizontally = scrollsHorizontally(settingsPane, CRAMPED_SETTINGS_PANE_WIDTH);

        assertFalse(overflow(settingsPane), scrollsHorizontally);
    }

    public void testCreateComponent_longSchemeName_needsNoHorizontalScrollBar()
    {
        useSchemes(scheme(LONG_SCHEME_NAME, "${className}Test", "test${subjectName}"));

        JScrollPane settingsPane = settingsPane();

        boolean scrollsHorizontally = scrollsHorizontally(settingsPane, CRAMPED_SETTINGS_PANE_WIDTH);

        assertFalse(overflow(settingsPane), scrollsHorizontally);
    }

    public void testCreateComponent_anyPage_reachesTheViewportAsTheWidthTrackingComponent()
    {
        Component view = settingsPane().getViewport().getView();

        boolean tracksViewportWidth =
            (view instanceof Scrollable) && ((Scrollable) view).getScrollableTracksViewportWidth();

        assertTrue(
            "the viewport holds a " + view.getClass().getName()
                + "; without Configurable.NoMargin the platform buries the page under a wrapper panel and the"
                + " viewport can no longer size it to its own width",
            tracksViewportWidth);
    }

    public void testUseProjectConfigCheckBox_toggled_keepsTheNewSelection()
    {
        JCheckBox useProjectConfig = useProjectConfigCheckBox(settingsPane());

        boolean initiallySelected = useProjectConfig.isSelected();

        useProjectConfig.doClick();

        assertEquals(
            "the checkbox reverted to its stored value, so the setting cannot be changed",
            !initiallySelected,
            useProjectConfig.isSelected());
    }

    private static JCheckBox useProjectConfigCheckBox(Container root)
    {
        String label = TestInsanityBundle.message("testinsanity.config.use", ProjectConfigParser.FILE_NAME);

        for (Component component : root.getComponents())
        {
            if ((component instanceof JCheckBox) && label.equals(((JCheckBox) component).getText()))
            {
                return (JCheckBox) component;
            }

            if (component instanceof Container)
            {
                JCheckBox found = useProjectConfigCheckBox((Container) component);

                if (found != null)
                {
                    return found;
                }
            }
        }

        return null;
    }

    private JScrollPane settingsPane()
    {
        return (JScrollPane) ConfigurableCardPanel.createConfigurableComponent(new TestInsanityConfig(getProject()));
    }

    private static boolean scrollsHorizontally(JScrollPane settingsPane, int width)
    {
        settingsPane.setSize(JBUI.scale(width), JBUI.scale(SETTINGS_PANE_HEIGHT));
        settingsPane.doLayout();

        return settingsPane.getHorizontalScrollBar().isVisible();
    }

    private static String overflow(JScrollPane settingsPane)
    {
        return "the page demands " + settingsPane.getViewport().getView().getPreferredSize().width
            + "px inside a " + settingsPane.getViewport().getWidth() + "px viewport";
    }
}
