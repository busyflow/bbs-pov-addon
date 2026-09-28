package Glaxium.POV.editor.section;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.editor.UIPovEditor;

public final class ActionsEditorSection implements PovEditorSection {
   public static final ActionsEditorSection INSTANCE = new ActionsEditorSection();

   private ActionsEditorSection() {
   }

   @Override
   public void fillSheets(UIPovEditor editor, boolean resetView) {
      RecordedPovActions actions = editor.getActions();
      editor.actionTimeline.setClips(actions);
   }
}
