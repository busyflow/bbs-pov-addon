package Glaxium.POV.config;

import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.ui.UIValueMap;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.joml.Vector4f;

public final class UICursorCropSetting {
   private UICursorCropSetting() {
   }

   public static void register() {
      UIValueMap.register(BakeToggleAllValue.class, (value, parent) -> {
         UIToggle toggle = UIValueFactory.booleanUI(value, t -> {
            PovSettings.setAllBake(t.getValue());
            if (parent instanceof UISettingsOverlayPanel panel) {
               panel.refresh();
            }
         });
         toggle.resetFlex();
         toggle.valueBinding(() -> toggle.setValue(PovSettings.areAllBakeEnabled()));
         return List.of(toggle);
      });
      UIValueMap.register(CursorTextureValue.class, (value, parent) -> {
         UIButton pick = new UIButton(IKey.constant("Pick Texture"), button -> UITexturePicker.open(parent.getContext(), (Link)value.get(), link -> {
               Link oldLink = (Link)value.get();
               value.set(link);
               if ((oldLink == null && link != null || oldLink != null && !oldLink.equals(link)) && PovSettings.cursorCrop != null) {
                  PovSettings.cursorCrop.set(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
               }
            }));
         pick.h(20);
         UIIcon clear = new UIIcon(Icons.CLOSE, button -> {
            value.set(null);
            if (PovSettings.cursorCrop != null) {
               PovSettings.cursorCrop.set(new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            }
         });
         clear.wh(20, 20);
         clear.tooltip(IKey.constant("Remove custom cursor texture"));
         UIElement row = UI.row(2, 0, 20, new UIElement[]{pick, clear}).h(20);
         row.w(90);
         row.valueBinding(() -> {
            Link link = (Link)value.get();
            if (link != null) {
               pick.label = IKey.constant("Tex: " + link.path);
               clear.setVisible(true);
            } else {
               pick.label = IKey.constant("Pick Texture");
               clear.setVisible(false);
            }
         });
         return List.of(UIValueFactory.column(row, value));
      });
      UIValueMap.register(CursorCropValue.class, (value, parent) -> {
         UIButton edit = new UIButton(IKey.constant("Edit Crop..."), button -> {
            Link link = PovSettings.cursorTexture == null ? null : (Link)PovSettings.cursorTexture.get();
            if (link != null) {
               Texture texture = BBSModClient.getTextures().getTexture(link);
               if (texture != null && texture.isValid() && texture.width > 0 && texture.height > 0) {
                  UIOverlay.addOverlay(parent.getContext(), new UICursorCropOverlayPanel(link, value), 0.5F, 0.5F);
               }
            }
         });
         edit.w(90);
         return List.of(UIValueFactory.column(edit, value));
      });
   }
}
