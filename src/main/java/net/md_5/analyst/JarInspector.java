package net.md_5.analyst;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import static net.md_5.analyst.ReferenceData.add;

public class JarInspector extends ClassVisitor {

    private final MethodReferenceFinder methodVisitor = new MethodReferenceFinder();
    private String myName;
    public ReferenceData referenceData = new ReferenceData();

    public JarInspector() {
        super(Opcodes.ASM4);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        myName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Ownable method = new Ownable(myName, name, desc);
        add(referenceData.methods, method);
        return methodVisitor;
    }

    private class MethodReferenceFinder extends MethodVisitor {

        public MethodReferenceFinder() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            Ownable field = new Ownable(owner, name, desc);
            add(referenceData.fields, field);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            Ownable method = new Ownable(owner, name, desc);
            add(referenceData.methods, method);
        }
    }
}
