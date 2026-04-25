import java.lang.reflect.*;
public class InspectMc {
  public static void main(String[] args) throws Exception {
    inspect("net.minecraft.client.gui.GuiGraphicsExtractor");
    inspect("net.minecraft.client.gui.Gui");
    inspect("net.minecraft.world.entity.player.Inventory");
  }
  static void inspect(String name) throws Exception {
    Class<?> c = Class.forName(name);
    System.out.println("=== " + name + " ===");
    for (Method m : c.getDeclaredMethods()) {
      System.out.println(Modifier.toString(m.getModifiers()) + " " + m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ") -> " + m.getReturnType().getName());
    }
  }
}
