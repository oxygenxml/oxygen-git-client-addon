package com.oxygenxml.git.view.renderer;

import java.awt.Color;
import java.lang.reflect.Method;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import com.oxygenxml.git.constants.Icons;
import com.oxygenxml.git.service.entities.GitChangeType;
import com.oxygenxml.git.translator.Tags;
import com.oxygenxml.git.translator.Translator;

public class RendererUtil {
  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RendererUtil.class);
  /**
   * Private constructor.
   */
  private RendererUtil() {}

  /**
   * Get the rendering info (such as icon or tooltip text) for the given Git change type.
   * 
   * @param changeType The Git change type.
   * 
   * @return the rendering info.
   */
  public static RenderingInfo getRenderingInfo(GitChangeType changeType) {
    Translator translator = Translator.getInstance();
    RenderingInfo renderingInfo = null;
    if (GitChangeType.ADD == changeType || GitChangeType.UNTRACKED == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_ADD_ICON),
          translator.getTranslation(Tags.ADD_ICON_TOOLTIP));
    } else if (GitChangeType.MODIFIED == changeType || GitChangeType.CHANGED == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_MODIFIED_ICON),
          translator.getTranslation(Tags.MODIFIED_ICON_TOOLTIP));
    } else if (GitChangeType.MISSING == changeType || GitChangeType.REMOVED == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_DELETE_ICON),
          translator.getTranslation(Tags.DELETE_ICON_TOOLTIP));
    } else if (GitChangeType.RENAME == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_RENAME_ICON),
          translator.getTranslation(Tags.RENAMED_ICON_TOOLTIP));
    } else if (GitChangeType.CONFLICT == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_CONFLICT_ICON),
          translator.getTranslation(Tags.CONFLICT_ICON_TOOLTIP));
    } else if (GitChangeType.SUBMODULE == changeType) {
      renderingInfo = new RenderingInfo(
          Icons.getIcon(Icons.GIT_SUBMODULE_FILE_ICON),
          translator.getTranslation(Tags.SUBMODULE_ICON_TOOLTIP));
    }
    return renderingInfo;
  }
  

  /**
   * Get inactive selection color.
   * 
   * @return the color.
   */
  public static Color getInactiveSelectionColor(JComponent comp, Color defaultColor) {
    Color inactiveBgColor = defaultColor;
    try {
      Class<?> colorProviderClass = Class.forName("ro.sync.ui.theme.SAThemeColorProvider");
      Object colorProvider = colorProviderClass.newInstance();
      Method getInactiveSelBgColorMethod = colorProviderClass.getMethod("getInactiveSelectionBgColor");
      int[] rgb = (int[]) getInactiveSelBgColorMethod.invoke(colorProvider);
      inactiveBgColor = new Color(rgb[0], rgb[1], rgb[2]);
    } catch (Exception e) {
      if (comp.isDoubleBuffered()) {
        logger.debug(e, e);
      }
    }
    return inactiveBgColor;
  }
  
}
