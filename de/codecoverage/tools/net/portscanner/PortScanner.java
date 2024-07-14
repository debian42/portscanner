/*
 * Frank Peters 2008
 * proof of concept for NIO
 */
package de.codecoverage.tools.net.portscanner;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.net.NetworkInterface;

public class PortScanner {
    
    private static final int WT_NOT_SAMENET = Integer.parseInt(System.getProperty("portscanner.tcp.autotune.notsame.wt", "1500"));
    private static final short FDS_NOT_SAMENET = Short.parseShort(System.getProperty("portscanner.tcp.autotune.notsame.fd", "64"));
    private static final int WT_SAMENET = Integer.parseInt(System.getProperty("portscanner.tcp.autotune.same.wt", "59"));
    private static final short FDS_SAMENET = Short.parseShort(System.getProperty("portscanner.tcp.autotune.same.fd", "740"));
    private static final int LOG_LEVEL=Integer.parseInt(System.getProperty("portscanner.loglevel", "0"));
    private static final ExecutorService tp = Executors.newFixedThreadPool(Integer.parseInt(System.getProperty("portscanner.tcp.connectscan.threads", "16")));
    private static final boolean TCP_CONNECT = Boolean.getBoolean("portscanner.tcp.connectscan");
    private static final List< Pair<InetAddress,Short>> IPLIST = new ArrayList< Pair<InetAddress,Short>>();
    private InetAddress hostAdress;
    private int start;
    private int end;
    private int waitTime;
    private short fds;
    
    private static class Pair <FIRST, SECOND> {
        private FIRST  first = null;
        private SECOND second = null;
    }
    
    private static class ConnectScan implements Callable<String> {
        private static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("portscanner.tcp.readtimeout", "3000"));
        private static final int MAX_READ_SOCKET = Integer.parseInt(System.getProperty("portscanner.tcp.sockbuf", "1024"));
        private static final int CONNECT_DELAY_VALUE = Integer.parseInt(System.getProperty("portscanner.tcp.connectdelay", "1"));
        private InetAddress addr = null;
        private int port = 0;
        private String value = "";
        ConnectScan(InetSocketAddress addr) {
            this.addr = addr.getAddress();
            this.port = addr.getPort();
        }
        public String call() throws Exception {
            Socket s=null;
            try {
                Thread.sleep(CONNECT_DELAY_VALUE);
                s = new Socket(addr, port);
                short cnt=0;       
                int rd=0;
                s.setSoTimeout(READ_TIMEOUT);
                while ( cnt<=MAX_READ_SOCKET && (rd=s.getInputStream().read()) != -1) {
                    value=value+(char)rd;
                }
            } catch (Throwable t) {
                //System.out.println(addr + ":" + port);
                //t.printStackTrace();
            }
            finally {
                if (s!=null) s.close();
            }
            return addr + ":" + port + " -> " + value;
        }

    }
    public PortScanner(String host, int sPort, int ePort) throws UnknownHostException {
        this(host, sPort, ePort, 1, (short)1, true);
    }
    public PortScanner(String host, int sPort, int ePort,int waitTime, short fds) throws UnknownHostException {
        this(host, sPort, ePort, waitTime, fds, false);
    }
    
    /** Creates new PortScanner 
     * @throws UnknownHostException */
    public PortScanner(String host, int sPort, int ePort,int waitTime, short fds, boolean auto) throws UnknownHostException {        
        this.fds=fds;
        this.waitTime=waitTime;
        this.hostAdress=InetAddress.getByName(host);
        this.start=sPort;
        this.end=ePort;
        System.setProperty("java.net.preferIPv4Stack","true");               
        if (auto) {
            initCards();
            if (LOG_LEVEL > 1) {
                System.out.println("Found IPs:");
                for (Pair<InetAddress, Short> p : IPLIST) {
                    System.out.println("IP:" + p.first.getHostAddress() + "/" + p.second.shortValue());
                }
            }
            boolean sameNet = checkNet(hostAdress);
            if (sameNet && LOG_LEVEL > 1)
                System.out.println("ip in same net");
            if (sameNet) {
                this.fds = FDS_SAMENET;
                this.waitTime = WT_SAMENET;
            } else {
                this.fds = FDS_NOT_SAMENET;
                this.waitTime = WT_NOT_SAMENET;
            }
            if (sameNet && LOG_LEVEL > 2)
                System.out.println("AutoTune on\nfds=" + this.fds+ "    waitTime=" + this.waitTime + " ms");
        }        
    }
    
	private boolean checkNet(InetAddress host) {
		byte[] ip = host.getAddress();
		long hostIP = (ip[0] & 0xFF) << 8;
			 hostIP = (hostIP + (ip[1] & 0xFF)) << 8;
			 hostIP = (hostIP + (ip[2] & 0xFF)) << 8;
			 hostIP = (hostIP + (ip[3] & 0xFF));
		if (LOG_LEVEL > 1) {
			System.out.println((ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF));
			System.out.println("hostIP     : " + hostIP);
			System.out.println("----------------");
		}
		for (int i = 0; i < IPLIST.size(); i++) {
			Pair<InetAddress, Short> p = IPLIST.get(i);
			byte[] internal = p.first.getAddress();
			long interIP = (internal[0] & 0xFF) << 8;
				 interIP = (interIP + (internal[1] & 0xFF)) << 8;
				 interIP = (interIP + (internal[2] & 0xFF)) << 8;
				 interIP = (interIP + (internal[3] & 0xFF));
			long matchingBits = ((long) Math.pow(2, p.second.shortValue())) - 1;
			matchingBits =  matchingBits << (32-p.second.shortValue());
			if (LOG_LEVEL > 1) {
				System.out.println("matchingBits: " + matchingBits);			
				System.out.println("interIP     : " + interIP);
				System.out.println((internal[0] & 0xFF) + "." + (internal[1] & 0xFF) + "." + (internal[2] & 0xFF) + "."	+ (internal[3] & 0xFF));
			}
			long zero = (interIP & matchingBits) ^ (hostIP & matchingBits);
			if (zero == 0) {
				return true;
			}
		}
		return false;
	}

    public void scan() throws IOException { 
        List<SocketChannel> pending = new ArrayList<SocketChannel>(1000);
        List<InetSocketAddress> found = new ArrayList<InetSocketAddress>(1000);
        List<Pair<Future<String>,InetSocketAddress>> foundFuture = new ArrayList<Pair<Future<String>,InetSocketAddress>>(1000);
        
        InetSocketAddress address;
        SocketChannel sc;
        Selector sel = Selector.open();
                
        int ac = 0;
        if ((end - start) > fds)
            ac = ((end - start) / fds) + 1;
        else
            ac = 1;
        long startTime = System.currentTimeMillis();
        if (LOG_LEVEL>1)
            System.out.println("rounds needed: " + ac);
        for (int tt = 0; tt < ac; tt++) {
            for (int ip = start + (tt * fds); (ip <= end) && (ip <= start -1 + (fds + (tt * fds))); ip++) {
                if (LOG_LEVEL>2) System.out.println("scan:" + hostAdress + ":" + ip);
                address = new InetSocketAddress(hostAdress,ip);
                sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.socket().setReuseAddress(true);
                try {
                    sc.connect(address);
                } catch (ConnectException c) {
                    if (LOG_LEVEL>1)
                        System.err.println(address+" -> "+c.getMessage() );
                    if (LOG_LEVEL>3)
                        c.printStackTrace();
                    sc.close();
                    continue;
                }
                pending.add(sc);                
                sc.register(sel, SelectionKey.OP_CONNECT, address);
            }
            while (sel.select(waitTime) != 0) {
                for (Iterator<?> i = sel.selectedKeys().iterator(); i.hasNext();) {
                    SelectionKey sk = (SelectionKey) i.next();
                    i.remove();
                    InetSocketAddress t = (InetSocketAddress) sk.attachment();
                    SocketChannel scd = (SocketChannel) sk.channel();
                    try {
                        if (scd.finishConnect()) {
                            sk.cancel();
                            scd.close();
                            if (LOG_LEVEL>0) {
                                System.out.print(t + "  found.");                            
                                if (TCP_CONNECT) System.out.print(" Checking connect/banner string");
                                System.out.println();
                            }
                            found.add(t);
                            if (TCP_CONNECT) { 
                                Pair<Future<String>, InetSocketAddress> ho=new Pair<Future<String>,InetSocketAddress>();
                                ho.second=t;                                
                                Future<String> f = tp.submit(new ConnectScan(t));
                                ho.first=f;
                                foundFuture.add(ho);
                            }
                        }
                    } catch (Exception x) {
                        sk.cancel();
                        scd.close();
                        if (LOG_LEVEL>3)
                            x.printStackTrace();
                        // System.out.println(x);
                    }
                }
            }
            for (Iterator<SocketChannel> i = pending.iterator(); i.hasNext();) {
                if (LOG_LEVEL >4) System.out.println("removing:"+i);
                SocketChannel sce = i.next();
                sce.close();
                i.remove();
            }
            sel.close();
            try {
                sel = Selector.open();
            }catch (Exception e) {
                    if (LOG_LEVEL >4) e.printStackTrace();
                    System.err.println("Probs: lower FDS and higher WAITTIME (sleeping 3s)");
                try {
                    Thread.sleep(3000);
                    sel = Selector.open();
                } catch (InterruptedException e1) {
                }
            }
        }
        System.out.println("\n\n###### List of open ports ######\n################################");
        int size = 0;
        if (TCP_CONNECT) {
            Collections.sort(foundFuture, new FoundFutureCmp());
            Iterator<Pair<Future<String>,InetSocketAddress>> i = foundFuture.iterator();            
            String retVal="";            
            for (; i.hasNext();) {
                Pair<Future<String>,InetSocketAddress> ho = i.next();
                try {                                                  
                    try {
                        retVal = ho.first.get();
                    } catch (InterruptedException e) { }                     
                    System.out.println("open <" +retVal + ">");
                } catch (ExecutionException e) {                
                    retVal = "ExecutionException";
                    System.out.println("open <" + ho.second+" ->"+retVal + ">");
                }                
                size++;
            }
        } else {
            Collections.sort(found, new FoundCmp());
            Iterator<InetSocketAddress> i = found.iterator();            
            for (; i.hasNext();) {
                InetSocketAddress add = i.next();
                System.out.println(add + " -> open");
                size++;           
            }       
        }
        System.out.println((System.currentTimeMillis() - startTime) / 1000 + " s to find " + size + " open ports from " + (end - start));
        System.exit(0);
    }

    private static void checkArgs(String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.err.println("Usage: java -jar portscanner.jar HOST start end [(wait in ms) (fds)]");
            System.err.println("Example: java -jar portscanner.jar localhost 1 10000 400 128\n Frank.Peters@CodeCoverage.de");
            System.exit(1);
        }

        try {
            InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.err.println("Host not found => " + args[0]);
            System.exit(2);
        } catch (SecurityException e) {
            System.err.println("SecurityException => " + args[0]);
            System.exit(2);
        }
        int start = 0;
        int end = 0;
        try {
            start = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Start port must be a number and between 1 - 65535");
            System.exit(3);
        }
        try {
            end = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("End port must be a number and between 1 - 65535");
            System.exit(3);
        }
        if (start < 1 || start > 65535) {
            System.err.println("Start port must be a number and between 1 - 65535");
            System.exit(3);
        }
        if (end < 1 || end > 65535) {
            System.err.println("End port must be a number and between 1 - 65535");
            System.exit(3);
        }
        if (start > end) {
            System.err.println("End port must not be greater than start port");
            System.exit(4);
        }
        if (args.length == 5) {
            try {
                Integer.parseInt(args[4]);
                Integer.parseInt(args[3]);                
            }catch (NumberFormatException e){
                System.err.println("fds and waittime must be a number");
                System.exit(14);
            }
        }
    }

    private void initCards() {
        Enumeration<NetworkInterface> cards=null;        
        try {            
            cards = NetworkInterface.getNetworkInterfaces();
        }catch (SocketException se) {
            System.err.println("Could not obtain network interfaces");
            System.exit(4);
        }
         while (cards.hasMoreElements()) {
             List<InterfaceAddress> IPAdresses = cards.nextElement().getInterfaceAddresses();             
             for(int i=0; i < IPAdresses.size(); i++) {
            	 InterfaceAddress a = IPAdresses.get(i);           	 
                 Pair<InetAddress,Short> p= new Pair<InetAddress,Short>();
                 p.first=a.getAddress();
                 p.second=a.getNetworkPrefixLength();
                 if (p.first.getAddress().length == 4)
                	 IPLIST.add(p);
             }
         }
    }
    
    // sort the stuff
    private static class FoundFutureCmp implements Comparator<Pair<Future<String>,InetSocketAddress>> {
        public int compare(Pair<Future<String>,InetSocketAddress> o1, Pair<Future<String>,InetSocketAddress> o2) {
            int retVal=0;
            if (o1.second.getPort() > o2.second.getPort()) retVal = 1;
            else if (o1.second.getPort() < o2.second.getPort()) retVal = -1;            
            return retVal;        
            }        
    }
    
    private static class FoundCmp implements Comparator<InetSocketAddress> {
        public int compare(InetSocketAddress o1, InetSocketAddress o2) {
            int retVal=0;
            if (o1.getPort() > o2.getPort()) retVal = 1;
            else if (o1.getPort() < o2.getPort()) retVal = -1;            
            return retVal;        
            }        
    }
    
    public static void main(String[] args) throws IOException {
        boolean auto=Boolean.getBoolean("portscanner.autotune");
        checkArgs(args);
        int start = Integer.parseInt(args[1]);
        int end = Integer.parseInt(args[2]);
        int wait=500;
        short fds=128;
        if (args.length == 5) {
            fds = Short.parseShort(args[4]);        
            wait = Integer.parseInt(args[3]);
        }
        PortScanner ps=null;
        if (auto)
            ps=new PortScanner(args[0], start, end);       
        else
            ps=new PortScanner(args[0], start, end, wait, fds);
        ps.scan();
    }
}
