package fortunedog.util;

public class Debug
{
	public static String whoCalledMe()
	{
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stackTraceElements[4];
		String classname = caller.getClassName();
		String methodName = caller.getMethodName();
		int lineNumber = caller.getLineNumber();
		return classname + "." + methodName + ":" + lineNumber;
	}

	public static void showCallStack()
	{
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (int i = 2; i < stackTraceElements.length; i++)
		{
			StackTraceElement ste = stackTraceElements[i];
			String classname = ste.getClassName();
			String methodName = ste.getMethodName();
			int lineNumber = ste.getLineNumber();
			System.out.println(classname + "." + methodName + ":" + lineNumber);
		}
	}
}
