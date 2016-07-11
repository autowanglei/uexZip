package org.zywx.wbpalmstar.plugin.uexzip;

import android.content.Context;
import android.widget.Toast;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.zip.CnZipInputStream;
import org.zywx.wbpalmstar.base.zip.CnZipOutputStream;
import org.zywx.wbpalmstar.base.zip.ZipEntry;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.AesZipFileDecrypter;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.AesZipFileEncrypter;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.impl.AESDecrypter;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.impl.AESDecrypterBC;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.impl.AESEncrypter;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.impl.AESEncrypterBC;
import org.zywx.wbpalmstar.plugin.uexzip.de.idyl.winzipaes.impl.ExtZipEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.DataFormatException;

public class EUExZip extends EUExBase {

    public static final String tag = "uexZip_";
    public static final String F_CALLBACK_NAME_ZIP = "uexZip.cbZip";
    public static final String F_CALLBACK_NAME_UNZIP = "uexZip.cbUnZip";

    public static final String m_encoding = "GBK";

    Context m_context;

    public EUExZip(Context context, EBrowserView inParent) {
        super(context, inParent);
        m_context = context;
    }

    private String getPath(String path) {

        if (path == null || path.length() == 0) {
            return null;
        }
        if (mBrwView.getCurrentWidget() == null) {
            return null;
        }

        return BUtility.makeRealPath(
                BUtility.makeUrl(mBrwView.getCurrentUrl(), path),
                mBrwView.getCurrentWidget().m_widgetPath,
                mBrwView.getCurrentWidget().m_wgtType);

    }

    /**
     * 将文件（或文件夹）进行压缩为指定的zip文件
     *
     * @throws Exception
     */
    public void zip(String[] parm) {
        if (parm.length < 2) {
            return;
        }
        int callbackId=-1;
        if (parm.length>2){
            callbackId= Integer.parseInt(parm[2]);
        }
        String inSrcPath = parm[0], inZippedPath = parm[1];
        final String newInSrcPath = getPath(inSrcPath);
        if (newInSrcPath == null) {
            cbZip(false,callbackId);
            return;
        }
        try {
            File file = new File(newInSrcPath);
            if (!file.exists()) {
                cbZip(false,callbackId);
                return;
            }
            final String newInZippedPath = getPath(inZippedPath);
            if (newInZippedPath == null) {
                return;
            }
            final int finalCallbackId = callbackId;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        zip(newInSrcPath, newInZippedPath, m_encoding, finalCallbackId);

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }).start();

        } catch (SecurityException e) {
            Toast.makeText(
                    m_context,
                    ResoureFinder.getInstance().getString(mContext,
                            "error_no_permisson_INTERNET"), Toast.LENGTH_SHORT)
                    .show();
        }

    }

    private void cbZip(boolean result, int callbackId){
        if (callbackId!=-1){
            callbackToJs(callbackId,false,result);
        }else {
            jsCallback(F_CALLBACK_NAME_ZIP, 0, EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
        }
    }

    public void zipWithPassword(String[] parm) {
        if (parm.length < 3) {
            return;
        }
        int callbackId=-1;
        if (parm.length>3){
            callbackId= Integer.parseInt(parm[3]);
        }
        final String inSrcPath = parm[0], inZippedPath = parm[1], inPassword = parm[2];
        final String newInSrcPath = getPath(inSrcPath);
        if (newInSrcPath == null) {
            cbZip(false,callbackId);
            return;
        }
        File file = new File(newInSrcPath);
        if (!file.exists()) {
            cbZip(false,callbackId);
            return;
        }
        final String newInZippedPath = getPath(inZippedPath);
        if (newInZippedPath == null) {
            return;
        }
        final int finalCallbackId = callbackId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    zipDirWithPassword(newInSrcPath, newInZippedPath, inPassword);
                    cbZip(true, finalCallbackId);
                } catch (Exception e) {
                    if (BDebug.DEBUG){
                        e.printStackTrace();
                    }
                    cbZip(false, finalCallbackId);
                }
            }
        }).start();
    }

    private void cbUnzip(boolean result,int callbackId){
        if (callbackId!=-1){
            callbackToJs(callbackId,false,result);
        }else{
            jsCallback(F_CALLBACK_NAME_UNZIP, 0, EUExCallback.F_C_INT,
                    EUExCallback.F_C_FAILED);
        }
    }


    /**
     * 将指定的zip文件解压到指定的目录下,如果文件已存在则覆盖。
     *
     * @throws Exception
     */
    public void unzip(String[] parm) {
        if (parm.length < 2) {
            return;
        }
        int callbackId=-1;
        if (parm.length>2){
            callbackId= Integer.parseInt(parm[2]);
        }
        String inSrcPath = parm[0], inUnzippedPath = parm[1];
        String newInSrcPath = getPath(inSrcPath);
        if (newInSrcPath == null) {
            cbUnzip(false,callbackId);
            return;
        }
        try {
            InputStream fileInputStream = null;
            String suffix = newInSrcPath.substring(newInSrcPath.lastIndexOf('.') + 1);
            if (!"zip".equalsIgnoreCase(suffix) && !"rar".equalsIgnoreCase(suffix)) {
                cbUnzip(false,callbackId);
                return;
            }
            if (newInSrcPath.startsWith("/")) {
                File file = new File(newInSrcPath);
                fileInputStream = new FileInputStream(file);
            } else {
                fileInputStream = mContext.getAssets().open(newInSrcPath);
            }
            final String newInUnzippedPath = getPath(inUnzippedPath);
            if (newInUnzippedPath == null) {
                cbUnzip(false,callbackId);
                return;
            }
            final InputStream finalFileInputStream = fileInputStream;
            final int finalCallbackId = callbackId;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    dezip(finalFileInputStream, newInUnzippedPath, m_encoding, finalCallbackId);
                }
            }).start();
        } catch (SecurityException e) {
            Toast.makeText(
                    m_context,
                    ResoureFinder.getInstance().getString(mContext,
                            "error_no_permisson_INTERNET"), Toast.LENGTH_SHORT)
                    .show();
            cbUnzip(false,callbackId);
        } catch (IOException e) {
            Toast.makeText(
                    m_context,
                    ResoureFinder.getInstance().getString(mContext,
                            "error_no_permisson_INTERNET"), Toast.LENGTH_SHORT)
                    .show();
            cbUnzip(false,callbackId);
            e.printStackTrace();
        }

    }

    public void unzipWithPassword(String[] parm) {
        if (parm.length < 3) {
            return;
        }
        int callbackId=-1;
        if (parm.length>3){
            callbackId= Integer.parseInt(parm[3]);
        }
        final String inSrcPath = parm[0], inUnzippedPath = parm[1], inPassword = parm[2];
        final String newInSrcPath = getPath(inSrcPath);
        if (newInSrcPath == null || !newInSrcPath.startsWith("/")) {
            cbUnzip(false,callbackId);
            return;
        }
        final int finalCallbackId = callbackId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String newInUnzippedPath = getPath(inUnzippedPath);
                    if (newInUnzippedPath == null) {
                        cbUnzip(false, finalCallbackId);
                        return;
                    }
                    unzipDirWithPassword(newInSrcPath, newInUnzippedPath, inPassword);
                    cbUnzip(true, finalCallbackId);
                } catch (Exception e) {
                    cbUnzip(false, finalCallbackId);
                }

            }
        }).start();

    }

    private void zip(String inputFileName, String outputFileName,
                     String encoding,int callbackId) throws Exception {
        if (encoding == null || encoding.equals(""))
            encoding = "UTF-8";

        File file = new File(outputFileName);
        if (file.exists()) {// 如果要压缩后的zip文件已经存在，则先将该文件删除
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        CnZipOutputStream out = new CnZipOutputStream(new FileOutputStream(
                outputFileName), encoding);
        zip(out, new File(inputFileName), "");
        out.close();
        cbZip(true,callbackId);
    }

    private void zip(CnZipOutputStream out, File f, String base)
            throws Exception {
        if (f.isDirectory()) {// 文件夹
            File[] fl = f.listFiles();
            out.putNextEntry(new ZipEntry(base + "/"));
            base = base.length() == 0 ? "" : base + "/";
            for (int i = 0; i < fl.length; i++) {
                System.out.println("i=" + i + "," + fl[i].getName());
                zip(out, fl[i], base + fl[i].getName());
            }
        } else {// 文件
            if (base.length() == 0) {// 当base为空时，表示是压缩指定的一个文件，否则就是压缩文件夹下的所有文件
                base = f.getName();
            }
            out.putNextEntry(new ZipEntry(base));
            FileInputStream in = new FileInputStream(f);
            int b;
            byte[] buffer = new byte[1024];// 提高文件压缩的速度
            while ((b = in.read(buffer)) != -1) {
                out.write(buffer, 0, b);
            }
            in.close();
        }
    }

    private void dezip(InputStream compress, String decompression,
                       String encoding, int callbackId) {
        if (encoding == null || encoding.equals(""))
            encoding = "UTF-8";
        File dir = new File(decompression);

        try {
            // 建立与目标文件的输入连接
            CnZipInputStream in = new CnZipInputStream(compress, encoding);
            ZipEntry file = in.getNextEntry();
            byte[] c = new byte[1024];
            int slen;
            while (file != null) {
                String zename = file.getName();
                if (file.isDirectory()) {
                    File files = new File(dir.getAbsolutePath() + "/" + zename); // 在指定解压路径下建子文件夹
                    files.mkdirs();// 新建文件夹
                } else {
                    File files = new File(dir.getAbsolutePath() + "/" + zename)
                            .getParentFile();// 当前文件所在目录
                    if (!files.exists()) {// 如果目录文件夹不存在，则创建
                        files.mkdirs();
                    }
                    FileOutputStream out = new FileOutputStream(
                            dir.getAbsolutePath() + "/" + zename);
                    while ((slen = in.read(c, 0, c.length)) != -1)
                        out.write(c, 0, slen);
                    out.close();
                }
                file = in.getNextEntry();
            }
            in.close();
            cbUnzip(true,callbackId);
        } catch (Exception i) {
            cbUnzip(false,callbackId);
        }

    }

    protected final AESDecrypter DECRYPTER = new AESDecrypterBC();

    public void unzipDirWithPassword(String sourceZipFile,
                                     String destinationDir, String password) throws IOException,
            DataFormatException {

        AesZipFileDecrypter aesDecryptor = new AesZipFileDecrypter(new File(
                sourceZipFile), DECRYPTER);
        List<ExtZipEntry> list = aesDecryptor.getEntryList();
        for (ExtZipEntry zip : list) {
            aesDecryptor.extractEntryWithTmpFile(zip, new File(destinationDir
                    + zip.getName()), password);
        }
        aesDecryptor.close();

    }

    protected final AESEncrypter encrypter = new AESEncrypterBC();

    public void zipDirWithPassword(String dirName, String zipFileName,
                                   String password) throws IOException {

        AesZipFileEncrypter enc = new AesZipFileEncrypter(zipFileName,
                encrypter);
        try {
            zip(enc, new File(dirName), "", password);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            enc.close();
        }

    }

    private void zip(AesZipFileEncrypter enc, File f, String base, String psw)
            throws Exception {
        if (f.isDirectory()) {// 文件夹
            File[] fl = f.listFiles();

            base = base.length() == 0 ? "" : base + "/";
            for (int i = 0; i < fl.length; i++) {
                zip(enc, fl[i], base + fl[i].getName(), psw);
            }
        } else {// 文件
            if (base.length() == 0) {// 当base为空时，表示是压缩指定的一个文件，否则就是压缩文件夹下的所有文件
                base = f.getName();
            }
            enc.add(base, new FileInputStream(f), psw);

        }
    }

    @Override
    protected boolean clean() {
        return false;
    }
}
