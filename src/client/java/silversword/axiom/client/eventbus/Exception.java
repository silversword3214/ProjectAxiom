package silversword.axiom.client.eventbus;

public class Exception extends RuntimeException {
    public Exception(Class<?> axiomClass) {
        super("No registered lambda listener for '" + axiomClass.getName() + "'.");
    }
}