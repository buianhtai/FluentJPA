package co.streamx.fluent.JPA;

import java.util.List;
import java.util.function.Function;

import co.streamx.fluent.JPA.DSLInterpreterHelpers.ParameterRef;
import co.streamx.fluent.JPA.JPAHelpers.Association;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

interface IdentifierPath extends UnboundCharSequence {
    CharSequence resolveInstance(CharSequence inst);

    CharSequence resolve(IdentifierPath path);

    CharSequence current();

    Class<?> getDeclaringClass();

    public static final char DOT = '.';

    public static CharSequence current(CharSequence seq) {
        return seq instanceof IdentifierPath ? ((IdentifierPath) seq).current() : seq;
    }

    public static boolean isResolved(CharSequence seq) {
        return seq instanceof Resolved;
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    @RequiredArgsConstructor
    public class Resolved implements IdentifierPath {
        private final CharSequence resolution;

        @Getter
        private final Class<?> declaringClass;

        @Override
        public int length() {
            return resolution.length();
        }

        @Override
        public char charAt(int index) {
            return resolution.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            return resolution.subSequence(start, end);
        }

        @Override
        public CharSequence resolveInstance(CharSequence inst) {
            if (inst instanceof ParameterRef)
                throw TranslationError.CANNOT_DEREFERENCE_PARAMETERS.getError(((ParameterRef) inst).getValue(),
                        resolution);
            if (Strings.isNullOrEmpty(inst))
                return this;
            if (inst instanceof IdentifierPath)
                return ((IdentifierPath) inst).resolve(this);
            return new Resolved(new StringBuilder(inst).append(DOT).append(resolution).toString(), declaringClass);
        }

        @Override
        public CharSequence resolve(IdentifierPath path) {
            return this;
        }

        @Override
        public CharSequence current() {
            return resolution;
        }

        @Override
        public String toString() {
            return resolution.toString();
        }
    }

    @RequiredArgsConstructor
    public class MultiColumnIdentifierPath implements IdentifierPath {

        private final String originalField;
        private final Function<Class<?>, JPAHelpers.Association> associationSupplier;
        private CharSequence inst;

        private RuntimeException error() {
            return new IllegalStateException("'" + originalField
                    + "' has multi-column mapping. You must call the appropriate property to resolve it");
        }

        @Override
        public Class<?> getDeclaringClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int length() {
            throw error();
        }

        @Override
        public char charAt(int index) {
            throw error();
        }

        @Override
        public CharSequence subSequence(int start,
                                        int end) {
            throw error();
        }

        @Override
        public String toString() {
            throw error();
        }

        @Override
        public CharSequence current() {
            return inst;
        }

        @Override
        public CharSequence resolveInstance(CharSequence inst) {
            if (this.inst != null)
                new IllegalStateException("Already initialized with '" + this.inst + "' instance. Passing a new '"
                        + inst + "' is illegal");
            this.inst = inst;
            return this;
        }

        @Override
        public CharSequence resolve(IdentifierPath path) {
            if (!(path instanceof Resolved))
                throw new IllegalArgumentException(path.getClass() + ":" + path.current());
            CharSequence key = path.current();
            Association association = associationSupplier.apply(path.getDeclaringClass());
            List<CharSequence> referenced = association.getRight();
            for (int i = 0; i < referenced.size(); i++) {
                CharSequence seq = referenced.get(i);
                if (Strings.equals(seq, key)) {
                    return new Resolved(new StringBuilder(inst).append(DOT).append(association.getLeft().get(i)),
                            path.getDeclaringClass());
                }
            }
            throw new IllegalArgumentException("Column '" + key + "' not found in PK: " + referenced);
        }

    }
}
