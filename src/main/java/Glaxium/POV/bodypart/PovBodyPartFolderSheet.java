package Glaxium.POV.bodypart;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet.Section;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class PovBodyPartFolderSheet extends UIKeyframeSheet {
   public PovBodyPartFolderSheet(String id, IKey title, KeyframeChannel<?> channel) {
      super(id, title, 0, channel, null);
      this.section = new Section(id, title, null, 0);
   }
}
