package com.quickblox.sample.videochatwebrtcnew.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.quickblox.chat.QBChatService;
import com.quickblox.sample.videochatwebrtcnew.R;
import com.quickblox.sample.videochatwebrtcnew.User;
import com.quickblox.sample.videochatwebrtcnew.activities.CallActivity;
import com.quickblox.sample.videochatwebrtcnew.adapters.OpponentsAdapter;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCConfig;
import com.quickblox.videochat.webrtc.QBRTCMediaConfig;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import org.jivesoftware.smack.SmackException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by tereha on 16.02.15.
 */
public class OpponentsFragment extends Fragment implements View.OnClickListener, Serializable {


    private static final String TAG = OpponentsFragment.class.getSimpleName();
    private OpponentsAdapter opponentsAdapter;
    public static String login;
    private Button btnAudioCall;
    private Button btnVideoCall;
    private View view=null;
    private ProgressDialog progresDialog;
    private ListView opponentsList;


    public static OpponentsFragment getInstance() {
        return new OpponentsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ((CallActivity)getActivity()).initActionBar();

        view = inflater.inflate(R.layout.fragment_opponents, container, false);

        initUI(view);

        // Show dialog till opponents loading
        progresDialog = new ProgressDialog(getActivity()) {
            @Override
            public void onBackPressed() {
                Toast.makeText(getActivity(), "Wait until loading finish", Toast.LENGTH_SHORT).show();
            }
        };
        progresDialog.setMessage("Load opponents ...");
        progresDialog.setCanceledOnTouchOutside(false);
        progresDialog.show();

        initOpponentListAdapter();
        // Get setting keys.
//         Log.d(TAG, "onCreateView() from OpponentsFragment Level 2");
        return view;
    }

    private void initOpponentListAdapter() {
        final ListView opponentsList = (ListView) view.findViewById(R.id.opponentsList);

        List<User> userList = new ArrayList<>(((CallActivity) getActivity()).getOpponentsList());
        prepareUserList(opponentsList, userList);
        progresDialog.dismiss();


    }

    private void prepareUserList(ListView opponentsList, List<User> users) {
        int i = searchIndexLogginedUser(users);
        if (i >= 0)
            users.remove(i);

        // Prepare users list for simple adapter.
        //
        opponentsAdapter = new OpponentsAdapter(getActivity(), users);
        opponentsList.setAdapter(opponentsAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        setRetainInstance(true);
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreate() from OpponentsFragment");
        super.onCreate(savedInstanceState);
    }

    private void initUI(View view) {

        login = getActivity().getIntent().getStringExtra("login");

        btnAudioCall = (Button)view.findViewById(R.id.btnAudioCall);
        btnVideoCall = (Button)view.findViewById(R.id.btnVideoCall);

        btnAudioCall.setOnClickListener(this);
        btnVideoCall.setOnClickListener(this);
        /*view.findViewById(R.id.crtChnnal).setOnClickListener(this);
        view.findViewById(R.id.closeChnnal).setOnClickListener(this);*/

        opponentsList = (ListView) view.findViewById(R.id.opponentsList);
    }

    @Override
    public void onClick(View v) {

        if (opponentsAdapter.getSelected().isEmpty()){
            Toast.makeText(getActivity(), "Choose one opponent", Toast.LENGTH_LONG).show();
            return;
        }

        if (opponentsAdapter.getSelected().size() > QBRTCConfig.getMaxOpponentsCount()){
            Toast.makeText(getActivity(), "Max number of opponents is 6", Toast.LENGTH_LONG).show();
            return;
        }
            QBRTCTypes.QBConferenceType qbConferenceType = null;

            //Init conference type
            switch (v.getId()) {
                case R.id.btnAudioCall:
                    qbConferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO;
                    break;

                case R.id.btnVideoCall:
                    // get call type
                    qbConferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO;

                    break;
                /*case R.id.crtChnnal:

                    break;
                case R.id.closeChnnal:
                    break;*/
            }

            defineRTCMediaSettings();

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("any_custom_data", "some data");
            userInfo.put("my_avatar_url", "avatar_reference");

            ((CallActivity) getActivity())
                    .addConversationFragmentStartCall(opponentsAdapter.getSelected(),
                            qbConferenceType, userInfo);

    }

    private void defineRTCMediaSettings() {
        SharedPreferences sharedPref = ((CallActivity)getActivity()).getDefaultSharedPrefs();

        // Check HW codec flag.
        boolean hwCodec = sharedPref.getBoolean(getString(R.string.pref_hwcodec_key),
                Boolean.valueOf(getString(R.string.pref_hwcodec_default)));

        QBRTCMediaConfig.setVideoHWAcceleration(hwCodec);
        // Get video resolution from settings.
        int resolutionItem = Integer.parseInt(sharedPref.getString(getString(R.string.pref_resolution_key),
                "0"));
        Log.e(TAG, "resolutionItem =: " + resolutionItem);
        for (QBRTCMediaConfig.QBRTCSessionVideoQuality quality : QBRTCMediaConfig.QBRTCSessionVideoQuality.values()){
            if (quality.ordinal() == resolutionItem){
                Log.e(TAG, "resolution =: " + quality.height + ":"+quality.width);
                QBRTCMediaConfig.setVideoHeight(quality.height);
                QBRTCMediaConfig.setVideoWidth(quality.width);
                break;
            }
        }

        // Get camera fps from settings.
        int fps = Integer.parseInt(sharedPref.getString(getString(R.string.pref_fps_key),
                "0"));
        Log.e(TAG, "cameraFps =: " + fps);
        QBRTCMediaConfig.setVideoFps(fps);

        // Get start bitrate.
        String bitrateTypeDefault = getString(R.string.pref_startbitrate_default);
        String bitrateType = sharedPref.getString(
                getString(R.string.pref_startbitrate_key), bitrateTypeDefault);
        if (!bitrateType.equals(bitrateTypeDefault)) {
            String bitrateValue = sharedPref.getString(getString(R.string.pref_startbitratevalue_key),
                    getString(R.string.pref_startbitratevalue_default));
            int startBitrate = Integer.parseInt(bitrateValue);
            QBRTCMediaConfig.setVideoStartBitrate(startBitrate);
        }

        int videoCodecItem = Integer.parseInt(getPreferenceString(sharedPref, R.string.pref_videocodec_key, "0"));
        for (QBRTCMediaConfig.VideoCodec codec : QBRTCMediaConfig.VideoCodec.values()){
            if (codec.ordinal() == videoCodecItem){
                Log.e(TAG, "videoCodecItem =: " + codec.getDescription());
                QBRTCMediaConfig.setVideoCodec(codec);
                break;
            }
        }

        String audioCodecDescription = getPreferenceString(sharedPref, R.string.pref_audiocodec_key,
                R.string.pref_audiocodec_def);
        QBRTCMediaConfig.AudioCodec audioCodec = QBRTCMediaConfig.AudioCodec.ISAC.getDescription()
                .equals(audioCodecDescription) ?
                    QBRTCMediaConfig.AudioCodec.ISAC : QBRTCMediaConfig.AudioCodec.OPUS;
        Log.e(TAG, "audioCodec =: " + audioCodec.getDescription());
        QBRTCMediaConfig.setAudioCodec(audioCodec);

    }

    private String getPreferenceString(SharedPreferences sharedPref , int StrRes, int StrResDefValue){
        return sharedPref.getString(getString(StrRes),
                getString(StrResDefValue));
    }

    private String getPreferenceString(SharedPreferences sharedPref , int StrRes, String defValue){
        return sharedPref.getString(getString(StrRes),
                defValue);
    }

    public static ArrayList<Integer> getOpponentsIds(List<QBUser> opponents){
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for(QBUser user : opponents){
            ids.add(user.getId());
        }
        return ids;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(progresDialog.isShowing()) {
            progresDialog.dismiss();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                try {
                    QBRTCClient.getInstance(getActivity()).destroy();
                    QBChatService.getInstance().logout();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
                getActivity().finish();
                return true;
            case R.id.settings:
                ((CallActivity)getActivity()).showSettings();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ArrayList<QBUser> reorderUsersByName(ArrayList<QBUser> qbUsers) {
        // Make clone collection to avoid modify input param qbUsers
        ArrayList<QBUser> resultList = new ArrayList<>(qbUsers.size());
        resultList.addAll(qbUsers);

        // Rearrange list by user IDs
        Collections.sort(resultList, new Comparator<QBUser>() {
            @Override
            public int compare(QBUser firstUsr, QBUser secondUsr) {
                if (firstUsr.getId().equals(secondUsr.getId())) {
                    return 0;
                } else if (firstUsr.getId() < secondUsr.getId()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return resultList;
    }

    public static int searchIndexLogginedUser(List<User> usersList) {
        int indexLogginedUser = -1;
        for (QBUser usr : usersList) {
            if (usr.getLogin().equals(login)) {
                indexLogginedUser = usersList.indexOf(usr);
                break;
            }
        }
        return indexLogginedUser;
    }
}