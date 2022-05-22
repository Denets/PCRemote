package com.denets.PCRemoteClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    public static MainActivity mainActivity;

    private String serIpAddress;
    private int port;

    private String command = "";
    private String args = "";

    private String res = "";
    private TextView res_txt = null;

    private SharedPreferences settings;
    private static final String PREFS_PROGRAMM = "Settings";

    private EditText etIPaddress;
    private EditText etPort;

    private ArrayList<String> files_desktop = new ArrayList<>();

    private ArrayList<String> groups_favorites = new ArrayList<>();
    private ArrayList<ArrayList<String>> files_favorites = new ArrayList<>();

    private Uri selectedFileUri;
    private String selectedFileName;
    private TextView txtChoosing;

    private final int REQUEST_PERMISSION = 101;

    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if(uri!=null) {
                            selectedFileUri = uri;

                            if(getPathFromUri(this, selectedFileUri) == null)
                                selectedFileName = "Wrong path";
                            else
                                selectedFileName = new File(getPathFromUri(this, selectedFileUri)).getName();

                            txtChoosing.setText(selectedFileName);
                        }
                        else{
                            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                        }
                    });

    class SenderThread extends AsyncTask<Void, Void, Void>
    {
        BufferedReader in;
        BufferedWriter out;
        Socket socket;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                setRes("Setting IP");
                InetAddress ipAddress = InetAddress.getByName(serIpAddress);

                setRes("Starting Socket");
                socket = new Socket();
                socket.connect(new InetSocketAddress(ipAddress, port), 1000);

                setRes("I/O settings");
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                setRes("Printing");
                out.write(command + " " + args);

                setRes("Printed");
                out.newLine();
                out.flush();

                setRes("Waiting result");
                res = in.readLine();
                if(getSupportFragmentManager().findFragmentById(R.id.fragmentMain).getClass() == Settings.class){
                    if(res.startsWith("SYSTEM") || res.startsWith("DESKTOP") || res.startsWith("FAVORITES")){
                        setRes("Can't display it");
                    }
                    else
                        setRes(res);
                }
                else{
                    if(res.startsWith("SYSTEM"))
                        setSystemInfo(in);
                    else if(res.startsWith("DESKTOP"))
                        getDesktop(in);
                    else if(res.startsWith("FAVORITES"))
                        getFavorites(in);
                    else
                        setRes(res);
                }
            } catch (UnknownHostException e) {
                setRes("Host error");
            } catch (IOException e) {
                setRes("Send error, please check host or restart app");
            } catch (Exception ex) {
                ex.printStackTrace();
                setRes("Unknown error");
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_PROGRAMM, MODE_PRIVATE);
        serIpAddress = settings.getString("IP", "");
        port = settings.getInt("Port", 8080);
        res_txt = findViewById(R.id.txtRes);

        setRes("This is output");

        mainActivity = this;
    }

    public void setRes(String res) { runOnUiThread(() -> { this.res = res; if(res_txt != null) res_txt.setText(res);}); }

    public void onClick (View v) {
        if (serIpAddress.isEmpty())
            Toast.makeText(this, "Введите ip адрес в настройках", Toast.LENGTH_SHORT).show();

        else {
            SenderThread sender = new SenderThread();
            args = "";
            switch (v.getId())
            {
                case R.id.btnMsg:
                    EditText etMsg = findViewById(R.id.etMsg);
                    args = etMsg.getText().toString();
                    if (!args.isEmpty()) {
                        command = "msg";
                        sender.execute();
                    }
                    else
                        Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnUrl:
                    EditText etUrl = findViewById(R.id.etUrl);
                    args = etUrl.getText().toString();
                    if (!args.isEmpty()) {
                        command = "url";
                        sender.execute();
                    }
                    else
                        Toast.makeText(this, "Введите url", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnCopy:
                    EditText etCopy = findViewById(R.id.etCopy);
                    args = etCopy.getText().toString();
                    if (!args.isEmpty()) {
                        command = "copy";
                        sender.execute();
                    }
                    else
                        Toast.makeText(this, "Введите текст", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnSearch:
                    EditText etSearch = findViewById(R.id.etSearch);
                    args = "google.com/search?q=" + etSearch.getText().toString();
                    if (!etSearch.getText().toString().isEmpty()) {
                        command = "url";
                        sender.execute();
                    }
                    else
                        Toast.makeText(this, "Введите запрос", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.btnStatsFrame:
                    command = "stats";
                    sender.execute();
                    break;
                case R.id.btnFile:
                case R.id.btnImage:
                    command = "file";
                    args = getFile();
                    sender.execute();
                    break;
            }
        }
    }

    public void onAdminClick(View v){
        EditText etCommand = findViewById(R.id.etCommand);
        String commandAdmin = etCommand.getText().toString();

        EditText etArgs = findViewById(R.id.etArgs);
        String argsAdmin = etArgs.getText().toString();

        if (serIpAddress.isEmpty())
            Toast.makeText(this, "Введите ip", Toast.LENGTH_SHORT).show();
        if(commandAdmin.isEmpty())
            Toast.makeText(this, "Введите комманду", Toast.LENGTH_SHORT).show();

        command = commandAdmin;
        args = argsAdmin;

        SenderThread sender = new SenderThread();
        sender.execute();
    }

    public void saveSettings(View v){
        etIPaddress = findViewById(R.id.etIPaddress);
        serIpAddress = etIPaddress.getText().toString();

        if(!etPort.getText().toString().isEmpty())
            port = Integer.parseInt(etPort.getText().toString());
        else
            Toast.makeText(getApplicationContext(), "Port пустой", Toast.LENGTH_SHORT).show();

        if (serIpAddress.isEmpty())
            Toast.makeText(getApplicationContext(), "IP пустой", Toast.LENGTH_SHORT).show();
        else{
            SharedPreferences.Editor save_editor = settings.edit();
            save_editor.putString("IP", serIpAddress);
            save_editor.putInt("Port", port);
            save_editor.apply();

            Toast.makeText(getApplicationContext(), "Applied", Toast.LENGTH_SHORT).show();
        }
    }

    public void setSystemInfo(BufferedReader in) throws IOException {
        String line;
        ArrayList<String> roots = new ArrayList<>();
        String root_res = "";
        while(!(line = in.readLine()).equals("system")){
            if(line.equals("root")){
                roots.add(root_res);
                root_res = "";
                continue;
            }
            root_res += line + "\n";
        }
        setGridInfo(roots, R.id.gridApps);


        ArrayList<String> system_info = new ArrayList<>();
        while(!(line = in.readLine()).equals("end")){
            system_info.add(line);
        }

        float total_ram = Float.parseFloat(system_info.get(0).split(" ")[system_info.get(0).split(" ").length - 1].replace(",", "."));
        float free_ram = Float.parseFloat(system_info.get(1).split(" ")[system_info.get(1).split(" ").length - 1].replace(",", "."));
        int cpu_load = Integer.parseInt(system_info.get(2).split(" ")[system_info.get(2).split(" ").length - 1]);

        ProgressBar pbRam = findViewById(R.id.pbRam);
        pbRam.setProgress(0);
        pbRam.setMax((int)(total_ram * 100));
        pbRam.setProgress((int)((total_ram - free_ram) * 100));

        pbRam.setOnClickListener(view -> Toast.makeText(getApplicationContext(), String.format("Total RAM: %s GB, Free RAM: %s GB", total_ram, free_ram), Toast.LENGTH_SHORT).show());

        ProgressBar pbCpu = findViewById(R.id.pbCpu);
        pbCpu.setProgress(cpu_load);

        pbCpu.setOnClickListener(view -> Toast.makeText(getApplicationContext(), "CPU load: " + cpu_load + "%", Toast.LENGTH_SHORT).show());

        setRes(" ");
    }

    private void setGridInfo(ArrayList<String> strings, int grid_id){
        runOnUiThread(() -> {
            GridView gridView = findViewById(grid_id);
            ArrayAdapter<String> adapter = new ArrayAdapter(getApplicationContext(), R.layout.root_list_item, strings);
            gridView.setAdapter(adapter);
        });
    }

    public void onFragmentButtonClick(View v){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        switch (v.getId()){
            case R.id.btnSettingsFrame:
                ft.replace(R.id.fragmentMain, new Settings());
                ft.commitNow();

                etIPaddress = findViewById(R.id.etIPaddress);
                etIPaddress.setText(serIpAddress);

                etPort = findViewById(R.id.etPort);
                etPort.setText(Integer.toString(port));
                break;
            case R.id.btnActionsFrame:
                ft.replace(R.id.fragmentMain, new Actions());
                ft.commit();
                break;
            case R.id.btnStatsFrame:
                ft.replace(R.id.fragmentMain, new Stats());
                ft.commitNow();
                onClick(v);
                break;
            case R.id.btnAppsFrame:
                ft.replace(R.id.fragmentMain, new Apps());
                ft.commitNow();

                SenderThread sender = new SenderThread();
                command = "favorites";
                args = "";
                sender.execute();

                setApps();
                break;
        }
    }

    public void getDesktop(BufferedReader in) throws IOException {
        String line;
        files_desktop = new ArrayList<>();

        while(!(line = in.readLine()).equals("END")){
            if(!line.equals("DESKTOP"))
                files_desktop.add(line);
        }

        setRes(" ");
    }

    public void setDesktop(){
        if(!files_desktop.isEmpty() && findViewById(R.id.lvDesktop) != null){
            ListView listView = findViewById(R.id.lvDesktop);
            ArrayAdapter<String> adapter = new ArrayAdapter(getApplicationContext(), R.layout.my_list_item, files_desktop);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> openFile(listView.getItemAtPosition(position).toString()));
        }
    }

    public void openFile(String name){
        Toast.makeText(getApplicationContext(), "Opening: " + name, Toast.LENGTH_SHORT).show();

        SenderThread st = new SenderThread();
        command = "open";
        args = name;
        st.execute();
    }

    public void getFavorites(BufferedReader in) throws IOException {
        SenderThread sender = new SenderThread();
        command = "desktop";
        args = "";
        sender.execute();

        groups_favorites = new ArrayList<>();
        files_favorites = new ArrayList<>();

        ArrayList<String> contents = new ArrayList<>();
        String line_in;
        while(!(line_in = in.readLine()).equals("END")){
            if(!line_in.equals("FAVORITES") && !line_in.isEmpty())
                contents.add(line_in);
        }

        String group;
        ArrayList<String> group_files = new ArrayList<>();
        for(String line : contents){
            if(line.startsWith("-")){
                files_favorites.add(group_files);
                group = line.replaceFirst("-", "");
                groups_favorites.add(group);
                group_files = new ArrayList<>();
            }
            else{
                if(line.contains(":")){
                    String name = line.split(":")[0].substring(0, line.split(":")[0].length()-2);
                    group_files.add(name);
                }
            }
        }

        files_favorites.add(group_files);
        files_favorites.remove(0);
    }

    public void setFavorites(){
        if(!files_favorites.isEmpty() && findViewById(R.id.rvFavoritesGroups) != null){
            RecyclerView rvGroups = findViewById(R.id.rvFavoritesGroups);
            FavoriteGroupAdapter fga = new FavoriteGroupAdapter(groups_favorites, files_favorites);

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            rvGroups.setAdapter(fga);
            rvGroups.setLayoutManager(linearLayoutManager);
        }
    }

    public void setApps(){
        AppsAdapter fa = new AppsAdapter(this);

        ViewPager2 vpFiles = findViewById(R.id.vpFiles);
        vpFiles.setAdapter(fa);

        TabLayout tlFiles = findViewById(R.id.tlFiles);
        new TabLayoutMediator(tlFiles, vpFiles, (tab, position) -> {tab.setText(fa.getFragments().get(position).getClass().getSimpleName());}).attach();
    }

    public void openFavorite(String name){
        Toast.makeText(getApplicationContext(), "Opening: " + name, Toast.LENGTH_SHORT).show();

        SenderThread st = new SenderThread();
        command = "favorite";
        args = name;
        st.execute();
    }

    public void onTextClick(View v){
        if(checkStoragePermission()){
            txtChoosing = (TextView) v;
            switch (v.getId()){
                case R.id.txtImage:
                    createIntent("image/*");
                    break;
                case R.id.txtFile:
                    createIntent("*/*");
                    break;
            }
        }
        else{
            Toast.makeText(this, "Grant permissions", Toast.LENGTH_SHORT).show();
        }
    }

    public void createIntent(String type){ mGetContent.launch(type); }

    public boolean checkStoragePermission(){
        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission refused", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getFile(){
        if(selectedFileName.isEmpty()){
            Toast.makeText(this, "Wrong file path", Toast.LENGTH_SHORT).show();
            return null;
        }

        try{
            byte[] bytes = readUri(this, selectedFileUri);

            return selectedFileName + " / " + Arrays.toString(bytes);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return null;
    }

    public byte[] readUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor pdf = context.getContentResolver().openFileDescriptor(uri, "r");

        assert pdf != null;
        assert pdf.getStatSize() <= Integer.MAX_VALUE;
        byte[] data = new byte[(int) pdf.getStatSize()];

        FileDescriptor fd = pdf.getFileDescriptor();
        FileInputStream fileStream = new FileInputStream(fd);
        fileStream.read(data);

        return data;
    }

    public String getPathFromUri(final Context context, final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && DocumentsContract.getDocumentId(uri).split(":")[0].startsWith("msf"))
                return getDataColumn(context, uri);

            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                return type + "/" + split[1];
            }
            else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri);
            }
            else if (isMediaDocument(uri)) {
                return getDataColumn(context, uri);
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri);
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public String getDataColumn(Context context, Uri uri) {
        final String[] projection = {MediaStore.Files.FileColumns.DISPLAY_NAME};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}