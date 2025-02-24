package com.xuanniao.reader.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.xuanniao.reader.R;
import com.xuanniao.reader.tools.Constants;

import static android.app.Activity.RESULT_OK;

public class SettingFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    static private String Tag = "SettingFragment";
    private Activity activity;
    private SharedPreferences sp = null;
    private Preference file_authorizeGet;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        //用于取值的SharedPreferences
        if (getActivity() != null) activity = getActivity();
        sp = PreferenceManager.getDefaultSharedPreferences(activity);
        initView();
    }

    private void initView() {
        file_authorizeGet = findPreference("file_authorizeGet");
        if (file_authorizeGet != null) {
            file_authorizeGet.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "file_authorizeGet":
                Toast.makeText(activity, "进入要选择的目录后，点击最下面的按钮即可", Toast.LENGTH_LONG).show();
                // 构造 DOCUMENTS 目录的 URI（需 URL 编码）
                String documentsPath = "primary:Documents"; // primary 表示主存储，Documents 对应公共目录
                String encodedPath = documentsPath.replace(":", "%3A").replace("/", "%2F");
                Uri documentsUri = Uri.parse("content://com.android.externalstorage.documents/tree/" + encodedPath);
                // 请求对该 URI 的持久化权限
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsUri);
                startActivityForResult(intent, Constants.FOLDER_SELECT_CODE);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.FOLDER_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            // 持久化 URI 权限
            activity.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            sp.edit().putString("file_authorize", uri.toString()).apply();
            Log.d(Tag, "uri:" + uri);
            // 后续操作可直接使用该 URI
//            createFolderUnderDocuments(uri, "NewFolder");
        }
    }

    public void createFolderUnderDocuments(Uri parentUri, String folderName) {
        DocumentFile parentDir = DocumentFile.fromTreeUri(activity, parentUri);
        if (parentDir != null && parentDir.exists()) {
            DocumentFile existingDir = parentDir.findFile(folderName);
            if (existingDir == null || !existingDir.isDirectory()) {
                // 创建新文件夹
                DocumentFile newDir = parentDir.createDirectory(folderName);
                if (newDir != null) {
                    Log.d("TAG", "文件夹创建成功: " + newDir.getUri());
                }
            }
        }
    }
}