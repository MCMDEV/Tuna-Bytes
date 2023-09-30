package io.tunabytes.bytecode;

import io.tunabytes.bytecode.editor.*;
import io.tunabytes.bytecode.introspect.MixinClassVisitor;
import io.tunabytes.bytecode.introspect.MixinInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class MixinsTransformer implements ClassFileTransformer {

    private final List<MixinsEditor> editors;
    private final MixinsConfig config;

    public MixinsTransformer() {
        this.editors = new ArrayList<>();
        editors.add(new DefinalizeEditor());
        editors.add(new OverwriteEditor());
        editors.add(new AccessorEditor());
        editors.add(new InjectionEditor());
        editors.add(new MethodMergerEditor());
        this.config = new MixinsConfig();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader targetReader = new ClassReader(classfileBuffer);
        ClassNode targetNode = new ClassNode();
        targetReader.accept(targetNode, ClassReader.SKIP_FRAMES);
        for (MixinEntry mixinEntry : config.getMixinEntries()) {
            if (!mixinEntry.getTargetClass().equals(className)) {
                continue;
            }

            ClassReader reader = mixinEntry.mixinReader();
            MixinClassVisitor visitor = new MixinClassVisitor();
            reader.accept(visitor, ClassReader.SKIP_FRAMES);
            MixinInfo info = visitor.getInfo();

            for (MixinsEditor editor : editors) {
                editor.edit(targetNode, info);
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        targetNode.accept(writer);

        return writer.toByteArray();
    }
}
