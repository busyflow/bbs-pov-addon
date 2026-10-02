package Glaxium.POV.editor.section;

import Glaxium.POV.editor.UIPovEditor;

/** One POV editor tab: HUD, Hand, BodyPart, or Actions. */
public interface PovEditorSection
{
    void fillSheets(UIPovEditor editor, boolean resetView);
}
