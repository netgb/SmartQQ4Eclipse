package cn.ieclipse.smartim.htmlconsole;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.wb.swt.SWTResourceManager;

import cn.ieclipse.smartim.IMPlugin;
import cn.ieclipse.smartim.htmlconsole.JSBridge.Callback;
import cn.ieclipse.smartim.preferences.HotKeyFieldEditor;
import cn.ieclipse.smartim.preferences.HotKeyPreferencePage;
import cn.ieclipse.util.StringUtils;

public class TabComposite extends Composite {
    
    private ToolBar toolBar;
    private SashForm sashForm;
    private Browser browser;
    private Text text;
    private IMChatConsole console;
    private boolean prepared = false;
    
    public TabComposite(IMChatConsole console) {
        this(console.getParent());
        this.console = console;
    }
    
    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     * @wbp.parser.constructor
     */
    public TabComposite(Composite parent) {
        super(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);
        
        toolBar = new ToolBar(this, SWT.FLAT | SWT.VERTICAL);
        toolBar.setBackground(SWTResourceManager
                .getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
        toolBar.setLayoutData(
                new GridData(SWT.CENTER, SWT.FILL, false, false, 1, 1));
                
        sashForm = new SashForm(this, SWT.VERTICAL);
        sashForm.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        sashForm.setSashWidth(6);
        
        browser = new Browser(sashForm, SWT.NONE);
        
        text = new Text(sashForm, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                // text.setTopIndex(text.getLineCount());
                resize(true);
            }
        });
        
        sashForm.addControlListener(new ControlListener() {
            @Override
            public void controlResized(ControlEvent e) {
                resize(false);
            }
            
            @Override
            public void controlMoved(ControlEvent e) {
            }
        });
        sashForm.setWeights(new int[] { 100, 1 });
        sashForm.pack();
        sashForm.layout();
        sashForm.setBackground(
                SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
        setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
        
        text.addKeyListener(inputListener);
        text.setBackground(
                SWTResourceManager.getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
        text.setToolTipText("请在此输入内容");
        // text.setText("正在加载聊天内容，在完成前请不要发送任何消息");
        
        browser.setJavascriptEnabled(true);
        boolean f = browser.setText(
                StringUtils.file2string(IMChatConsole.class, "history.html"),
                true);
        new JSBridge(browser, "prepared").setCallback(new Callback() {
            @Override
            public Object onFunction(Object[] args) {
                prepared = true;
                text.setText("");
                return null;
            }
        });
        browser.addProgressListener(new ProgressListener() {
            
            @Override
            public void completed(ProgressEvent event) {
                prepared = true;
            }
            
            @Override
            public void changed(ProgressEvent event) {
            
            }
        });
        browser.addLocationListener(new LocationListener() {
            
            @Override
            public void changing(LocationEvent event) {
                event.doit = false;
                if (console != null) {
                    String url = event.location;
                    if (url.startsWith("user://") && url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    console.hyperlinkActivated(url);
                }
            }
            
            @Override
            public void changed(LocationEvent event) {
                // TODO Auto-generated method stub
                
            }
        });
        browser.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent event) {
                if (event.button == 3)
                    browser.execute(
                            "document.oncontextmenu = function() {return false;}");
            }
        });
    }
    
    private void resize(boolean auto) {
        Point p = sashForm.getSize();
        int minHeight = Math.min(getInputHeight(), p.y - 20);
        sashForm.setWeights(new int[] { p.y - minHeight, minHeight });
        if (auto) {
            text.setTopIndex(Integer.MAX_VALUE);
        }
    }
    
    private int getInputHeight() {
        return text.getLineCount() * text.getLineHeight() + 2;
    }
    
    private KeyListener inputListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            String key = HotKeyFieldEditor.keyEvent2String(e);
            IPreferenceStore store = IMPlugin.getDefault().getPreferenceStore();
            if (key.equals(store.getString(HotKeyPreferencePage.KEY_SEND))) {
                e.doit = false;
                String input = text.getText();
                if (console != null && !input.isEmpty()) {
                    console.send(input.trim());
                }
                text.setText("");
            }
            else if (key
                    .equals(store.getString(HotKeyPreferencePage.KEY_HIDE))) {
                e.doit = false;
                if (console != null) {
                    console.hideAll();
                }
            }
            else if (key.equals(
                    store.getString(HotKeyPreferencePage.KEY_HIDE_CLOSE))) {
                e.doit = false;
                if (console != null) {
                    console.dispose();
                }
            }
            else if (key.equals(
                    store.getString(HotKeyPreferencePage.KEY_INPUT_ESC))) {
                e.doit = false;
            }
        }
    };
    
    private KeyListener inputListener2 = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            String key = HotKeyFieldEditor.keyEvent2String(e);
            if (key.equals("CR")) {
                e.doit = false;
                String input = text.getText();
                if (!input.isEmpty()) {
                    addHistory(input.trim(), true);
                }
                text.setText("");
            }
        }
    };
    
    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
    
    public Browser getHistoryWidget() {
        return browser;
    }
    
    public Text getInputWidget() {
        return text;
    }
    
    public ToolBar getToolBar() {
        return toolBar;
    }
    
    private boolean isReady() {
        if (prepared) {
            return true;
        }
        return false;
    }
    
    private void checkBrowser() {
        try {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    int count = 20;
                    while (!isReady()) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        count--;
                        if (count <= 0) {
                            break;
                        }
                    }
                }
            };
            thread.start();
            thread.join(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void addHistory(String msg, boolean scrollLock) {
        if (msg == null) {
            return;
        }
        if (!prepared) {
            checkBrowser();
        }
        // System.out.println("prepared:" + prepared);
        String text = msg.replace("'", "&apos;").replaceAll("\r?\n", "");
        final StringBuilder sb = new StringBuilder();
        sb.append("add_log('");
        sb.append(text);
        sb.append("'");
        
        if (!scrollLock) {
            sb.append(", true");
        }
        sb.append(");");
        IMPlugin.runOnUI(new Runnable() {
            @Override
            public void run() {
                appendHistory(sb.toString());
            }
        });
    }
    
    public void appendHistory(String text) {
        if (!browser.execute(text)) {
            addHistory("<div class=\"error\">添加到聊天记录失败，可能是因为消息中包含某些特殊字符引</div>",
                    true);
        }
    }
    
    public void clearHistory() {
        browser.execute("clear_log()");
    }
    
    public static void main(String[] args) {
        try {
            Display display = Display.getDefault();
            Shell shell = new Shell(display);
            shell.setSize(new Point(300, 200));
            shell.setLayout(new FillLayout());
            TabComposite comp = new TabComposite(shell);
            comp.getInputWidget().removeKeyListener(comp.inputListener);
            comp.getInputWidget().addKeyListener(comp.inputListener2);
            shell.open();
            shell.layout();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
