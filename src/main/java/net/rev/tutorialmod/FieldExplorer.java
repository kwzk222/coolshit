package net.rev.tutorialmod;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.Camera;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FieldExplorer {
    public static void dump() {
        System.out.println("--- Entity Fields ---");
        for (Field f : Entity.class.getDeclaredFields()) {
            System.out.println(f.getName());
        }
        System.out.println("--- Entity Methods ---");
        for (Method m : Entity.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
        System.out.println("--- Camera Methods ---");
        for (Method m : Camera.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}
