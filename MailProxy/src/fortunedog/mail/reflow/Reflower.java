package fortunedog.mail.reflow;

import java.io.InputStream;


public interface Reflower 
{
	ContentPager reflow(InputStream in);
}
