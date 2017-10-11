package fortunedog.mail.proxy;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.URLName;
/*
* Demo app that exercises the Message interfaces.
* Show information about and contents of messages.
*
* msgshow protocol host user password mailbox msgnum
*/
public class msgshow {
	static String protocol;
	static String host = null;
	static String user = null;
	static String password = null;
	static String mbox = "INBOX";
	static String url = null;
	static int port = -1;
	static boolean verbose = true;
	static boolean debug = false;
	static boolean showStructure = false;
	static boolean useSSL = false;
	static PrintStream out = System.out;
	
	@SuppressWarnings("deprecation")
	public static void main(String argv[]) {
		int msgnum = -1;
		int optind;
		for (optind = 0; optind < argv.length; optind++) {
			if (argv[optind].equals("-T")) {
				protocol = argv[++optind];
			} else if (argv[optind].equals("-H")) {
				host = argv[++optind];
			} else if (argv[optind].equals("-U")) {
				user = argv[++optind];
			} else if (argv[optind].equals("-P")) {
				password = argv[++optind];
			} else if (argv[optind].equals("-v")) {
				verbose = true;
			} else if (argv[optind].equals("-D")) {
				debug = true;
			} else if (argv[optind].equals("-f")) {
				mbox = argv[++optind];
			} else if (argv[optind].equals("-L")) {
				url = argv[++optind];
			} else if (argv[optind].equals("-p")) {
				port = Integer.parseInt(argv[++optind]);
			} else if (argv[optind].equals("-s")) {
				showStructure = true;
			} else if (argv[optind].equals("-SSL")) {
				useSSL = true;
			} else if (argv[optind].equals("--")) {
				optind++;
				break;
			} else if (argv[optind].startsWith("-")) {
				System.out.println(
				"Usage: msgshow [-L url] [-T protocol] [-H host] [-p port] [-U user]");
				System.out.println(
				" [-P password] [-f mailbox] [msgnum] [-v] [-D] [-s]");
				System.exit(1);
			} else {
				break;
			}
		}
		try {
			if (optind < argv.length)
				msgnum = Integer.parseInt(argv[optind]);
			// Get a Properties object
			Properties props = System.getProperties();
			if(useSSL)
			{// SSL 连接需要(开始)

				Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());  
				final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";  
				props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);  
				props.setProperty("mail.pop3.socketFactory.fallback", "false");  
				props.setProperty("mail.pop3.port", "995");  
				props.setProperty("mail.pop3.socketFactory.port", "995");  			// Get a Session object
			}// SSL连接需要(结束)
			Session session = Session.getDefaultInstance(props, null);
			session.setDebug(debug);
			// Get a Store object
			Store store = null;
			if (url != null) {
				URLName urln = new URLName(url);
				store = session.getStore(urln);
				store.connect();
			} else {
				if (protocol != null)
					store = session.getStore(protocol);
				else
					store = session.getStore();
				// Connect
				if (host != null || user != null || password != null)
					store.connect(host, port, user, password);
				else
					store.connect();
			}
			// Open the Folder
			Folder folder = store.getDefaultFolder();
			if (folder == null) {
				System.out.println("No default folder");
				System.exit(1);
			}
			
			folder = folder.getFolder(mbox);
			if (folder == null) {
				System.out.println("Invalid folder");
				System.exit(1);
			}
			// try to open read/write and if that fails try read-only
			try {
				folder.open(Folder.READ_WRITE);
			} catch (MessagingException ex) {
				folder.open(Folder.READ_ONLY);
			}
			int totalMessages = folder.getMessageCount();
			int newMessages = folder.getNewMessageCount();
			System.out.println("Total messages : " + totalMessages);
			System.out.println("New messages : " + newMessages);
			System.out.println("-------------------------------");
			
			if (msgnum == -1) {
				// Attributes & Flags for all messages ..
				//Message[] msgs = folder.getMessages(totalMessages-5, totalMessages);
				long s1 = System.currentTimeMillis();
				Message[] msgs = folder.getMessages();
				long s2 = System.currentTimeMillis();
//				final Date afterDate = new Date(Date.parse("Tue Jan 19 10:05:44 CST 2010"));
//				Message[] msgs = folder.search(new SentDateTerm(SentDateTerm.GT, afterDate));
				long s3 = System.currentTimeMillis();
				System.out.println("Get all use time:"+ (s2 - s1) +" search use time:" + (s3 - s2));
				// Use a suitable FetchProfile
				FetchProfile fp = new FetchProfile();
				fp.add(UIDFolder.FetchProfileItem.UID);
//				fp.add(FetchProfile.Item.ENVELOPE);
//				fp.add(FetchProfile.Item.FLAGS);
//				fp.add("X-Mailer");
//				fp.add("Message-Id");
				long s4 = System.currentTimeMillis();
				folder.fetch(msgs, fp);
				long s5 = System.currentTimeMillis();
				//sort message accord date
				long s6 = System.currentTimeMillis();
				System.out.println("Fetch spend:" + (s5 - s4));
				System.out.println("sort spend:" + (s6 - s5));

				//				com.sun.mail.pop3.POP3Folder sunFolder = (com.sun.mail.pop3.POP3Folder)folder;
				org.apache.geronimo.javamail.store.pop3.POP3Folder sunFolder = (org.apache.geronimo.javamail.store.pop3.POP3Folder)folder;
				out = new PrintStream("D:\\mails\\mail_dump.txt");
				for (int i = 10; i < 13 /* && i>= msgs.length - 10 */; i++)
//				int i = 352;
				{
					System.out.println("--------------------------");
					System.out.println("MESSAGE #" + msgs[i].getMessageNumber() + ":");
					out.println("--------------------------");
					out.println("MESSAGE #" + msgs[i].getMessageNumber() + ":");
					
					try
					{
						//						com.sun.mail.pop3.POP3Message sunMsg = (com.sun.mail.pop3.POP3Message) msgs[i];
						org.apache.geronimo.javamail.store.pop3.POP3Message sunMsg = (org.apache.geronimo.javamail.store.pop3.POP3Message) msgs[i];
						String uid = sunFolder.getUID(sunMsg);
						System.out.println("UID:" + uid);
						msgs[i].writeTo(new FileOutputStream("D:\\mails\\msg"+i+".eml"));
						dumpEnvelope(msgs[i], true);
						dumpPart(msgs[i], out);
					}
					catch (Throwable e)
					{
						out.println("\nException:" + e.getMessage());
					}
					msgs[i] = null;
					
				}
				out.close();
				out = System.out;
//				if(msgs.length > 0)
//					dumpPart(msgs[352]);
			}

			folder.close(false);
			store.close();
		} 
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		System.exit(1);
	}
	
	public static void dumpPart(Part p, PrintStream out) throws Exception {
		/**
		 * Dump input stream .. InputStream is = p.getInputStream(); // If "is"
		 * is not already buffered, wrap a BufferedInputStream // around it. if
		 * (!(is instanceof BufferedInputStream)) is = new
		 * BufferedInputStream(is); int c; while ((c = is.read()) != -1)
		 * System.out.write(c);
		 */
		pr("CONTENT-TYPE: " + p.getContentType());
		/*
		 * Using isMimeType to determine the content type avoids fetching the
		 * actual content data until we need it.
		 */
		if (p.isMimeType("text/plain")) {
			pr("This is plain text");
			pr("---------------------------");
			if (!showStructure)
				out.println((String)p.getContent());
		} else if (p.isMimeType("multipart/*")) {
			pr("This is a Multipart");
			pr("---------------------------");
			Multipart mp = (Multipart)p.getContent();
			level++;
			int count = mp.getCount();
			for (int i = 0; i < count; i++)
				dumpPart(mp.getBodyPart(i), out);
			level--;
		} else if (p.isMimeType("message/rfc822")) {
			pr("This is a Nested Message");
			pr("---------------------------");
			level++;
			dumpPart((Part)p.getContent(), out);
			level--;
		} else if (!showStructure) {
			/*
			 * If we actually want to see the data, and it’s not a MIME type we
			 * know, fetch it and check its Java type.
			 */
			Object o = p.getContent();
			if (o instanceof String) {
				pr("This is a string");
				pr("---------------------------");
				out.println((String)o);
			} else if (o instanceof InputStream) {
				pr("This is just an input stream");
				pr("---------------------------");
				pr("file name:"+javax.mail.internet.MimeUtility.decodeText(p.getFileName()));
//				InputStream is = (InputStream)o;
//				int c;
//				while ((c = is.read()) != -1)
//					out.write(c);
			} else {
				pr("This is an unknown type:"+o.getClass().getName());
				pr("---------------------------");
				pr(o.toString());
			}
		} else {
			pr("This is an unknown type:" );
			pr("---------------------------");
		}
	}
	public static void dumpEnvelope(Message m) throws Exception {
		dumpEnvelope(m, false);
	}
	public static void dumpEnvelope(Message m, boolean dumpHead) throws Exception {
		pr("This is the message envelope");
		pr("---------------------------");
		Address[] a;
		// FROM
		if ((a = m.getFrom()) != null) {
			for (int j = 0; j < a.length; j++)
				pr("FROM: " + javax.mail.internet.MimeUtility.decodeText(a[j].toString()));
		}
		// TO
		if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
			for (int j = 0; j < a.length; j++)
				pr("TO: " + javax.mail.internet.MimeUtility.decodeText(a[j].toString()));
		}
		// SUBJECT
		pr("SUBJECT: " + m.getSubject());
		// DATE
		Date d = m.getReceivedDate();
		pr("ReceivedDate: " +
				(d != null ? d.toString() : "UNKNOWN"));
		Date d2 = m.getSentDate();
		pr("SentDate: " +
				(d2 != null ? d2.toString() : "UNKNOWN"));
		

		HashSet<String> interestHead = new HashSet<String>(Arrays.asList("Date", "Message-ID" ));

		if(dumpHead)
		{
			@SuppressWarnings("unchecked")
			Enumeration<Header> eu = m.getAllHeaders();
			while(eu.hasMoreElements())
			{
				Header head = eu.nextElement();
//				if(interestHead.contains(head.getName()))
					out.println(head.getName() +":"+head.getValue());
			}
		}
	}
	
	static String indentStr = "                                                                ";
	static int level = 0;
	/**
	 * Print a, possibly indented, string.
	 */
	public static void pr(String s) {
		if (showStructure)
			out.print(indentStr.substring(0, level * 2));
		out.println(s);
	}
		
		
	}