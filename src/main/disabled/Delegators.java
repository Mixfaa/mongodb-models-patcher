package disabled;

import com.mixfa.mongopatcher.Patch;
import net.bytebuddy.implementation.bind.annotation.Argument;

import java.lang.invoke.MethodHandle;

public class Delegators {
    public static record Delegator0(
            String field,
            MethodHandle methodHandle
    ) {

        public Patch method() throws Throwable {
            return (Patch) methodHandle.invoke(new Patch(), field);
        }
    }

    public static record Delegator1(
            String field,
            MethodHandle methodHandle
    ) {
        public Patch method(@Argument(0) Object arg1) throws Throwable {
            return (Patch) methodHandle.invoke(new Patch(), field, arg1);
        }
    }

    public static record Delegator2(
            String field,
            MethodHandle methodHandle
    ) {
        public Patch method(@Argument(0) Object arg1, @Argument(1) Object arg2) throws Throwable {
            return (Patch) methodHandle.invoke(new Patch(), field, arg1, arg2);
        }
    }

    public static record Delegator3(
            String field,
            MethodHandle methodHandle
    ) {
        public Patch method(@Argument(0) Object arg1, @Argument(1) Object arg2, @Argument(2) Object arg3) throws Throwable {
            return (Patch) methodHandle.invoke(new Patch(), field, arg1, arg2, arg3);
        }
    }
}
