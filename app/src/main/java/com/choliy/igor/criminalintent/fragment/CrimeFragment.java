package com.choliy.igor.criminalintent.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.choliy.igor.criminalintent.Crime;
import com.choliy.igor.criminalintent.CrimeUtils;
import com.choliy.igor.criminalintent.R;
import com.choliy.igor.criminalintent.data.CrimeConstants;
import com.choliy.igor.criminalintent.data.CrimeLab;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment implements
        TextWatcher,
        View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private ImageView mPhotoPicker;
    private EditText mCrimeTitle;
    private Button mDateButton;
    private Button mTimeButton;
    private Button mSuspectButton;
    private Button mReportButton;
    private CheckBox mSolvedCheckBox;

    private Crime mCrime;
    private File mPhotoFile;
    private Intent mPickContact;
    private Intent mCapturePhoto;

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(CrimeConstants.ARG_CRIME_ID, crimeId);
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(CrimeConstants.ARG_CRIME_ID);
        Context context = getActivity();
        mCrime = CrimeLab.getInstance(context).getCrime(crimeId);
        mPhotoFile = CrimeLab.getInstance(context).getPhotoFile(context, mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crime, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PackageManager packageManager = getActivity().getPackageManager();

        // Setup Crime photo picker
        mPhotoPicker = (ImageView) view.findViewById(R.id.crimePhoto);
        mPhotoPicker.setOnClickListener(this);
        mCapturePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // If device has no camera or no free space for photo,
        // set photoPicker enabled.
        boolean canTakePhoto = mPhotoFile != null &&
                mCapturePhoto.resolveActivity(packageManager) != null;
        mPhotoPicker.setEnabled(canTakePhoto);

        // If device has camera and free space,
        // put photoUri to intent.
        if (canTakePhoto) {
            Uri photoUri;
            if (Build.VERSION.SDK_INT >= 24) {
                photoUri = FileProvider.getUriForFile(
                        getActivity(),
                        getActivity().getApplicationContext().getPackageName() + ".provider",
                        mPhotoFile);
            } else photoUri = Uri.fromFile(mPhotoFile);

            mCapturePhoto.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        }

        // Setup Crime title
        mCrimeTitle = (EditText) view.findViewById(R.id.crimeTitle);
        mCrimeTitle.addTextChangedListener(this);

        // Setup Crime date button
        mDateButton = (Button) view.findViewById(R.id.crimeDate);
        mDateButton.setOnClickListener(this);

        // Setup Crime time button
        mTimeButton = (Button) view.findViewById(R.id.crimeTime);
        mTimeButton.setOnClickListener(this);

        // Setup Crime suspend button
        mSuspectButton = (Button) view.findViewById(R.id.crimeSuspect);
        mSuspectButton.setOnClickListener(this);
        if (mCrime.getSuspect() != null)
            mSuspectButton.setText(mCrime.getSuspect());

        mPickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        if (packageManager.resolveActivity(mPickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setText(R.string.crime_suspect_error);
            mSuspectButton.setEnabled(false);
        }

        // Setup Crime report button
        mReportButton = (Button) view.findViewById(R.id.crimeReport);
        mReportButton.setOnClickListener(this);

        // Setup Crime solved checkBox
        mSolvedCheckBox = (CheckBox) view.findViewById(R.id.crimeSolved);
        mSolvedCheckBox.setOnCheckedChangeListener(this);

        // Update Crime
        mCrimeTitle.setText(mCrime.getTitle());
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        updateDate();
        updateTime();
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.getInstance(getActivity()).updateCrime(mCrime);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;

        switch (requestCode) {
            case CrimeConstants.REQUEST_CODE_PICKER:
                Date date = (Date) data.getSerializableExtra(CrimeConstants.EXTRA_DATE_TIME);
                mCrime.setDate(date);
                updateDate();
                updateTime();
                break;
            case CrimeConstants.REQUEST_CODE_CONTACT:
                if (data == null) break;
                Uri contactUri = data.getData();

                // Contact names
                String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME};

                // Bind all query fields to the cursor
                Cursor cursor = getActivity()
                        .getContentResolver()
                        .query(contactUri, queryFields, null, null, null);
                try {
                    assert cursor != null;
                    if (cursor.getCount() == 0) return;
                    cursor.moveToFirst();

                    // Get data from the first row - name of suspect
                    String suspect = cursor.getString(0);
                    mCrime.setSuspect(suspect);
                    mSuspectButton.setText(suspect);
                } finally {
                    assert cursor != null;
                    cursor.close();
                }
                break;
            case CrimeConstants.REQUEST_CODE_PHOTO:
                // TODO
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        mCrime.setTitle(charSequence.toString());
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.crimePhoto:
                startActivityForResult(mCapturePhoto, CrimeConstants.REQUEST_CODE_PHOTO);
                break;
            case R.id.crimeDate:
                showPicker(CrimeConstants.DATE_PICKER_TYPE);
                break;
            case R.id.crimeTime:
                showPicker(CrimeConstants.TIME_PICKER_TYPE);
                break;
            case R.id.crimeSuspect:
                startActivityForResult(mPickContact, CrimeConstants.REQUEST_CODE_CONTACT);
                break;
            case R.id.crimeReport:
                Intent intent = ShareCompat.IntentBuilder.from(getActivity())
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setType("text/plain")
                        .setChooserTitle(getString(R.string.crime_report_send))
                        .createChooserIntent();
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        mCrime.setSolved(isChecked);
    }

    private String getCrimeReport() {
        String solvedString;
        if (mCrime.isSolved())
            solvedString = getString(R.string.crime_report_solved);
        else
            solvedString = getString(R.string.crime_report_unsolved);

        String dateString = CrimeUtils.formatCrimeReport(getActivity(), mCrime.getDate());

        String suspect = mCrime.getSuspect();
        if (suspect == null)
            suspect = getString(R.string.crime_report_no_suspect);
        else
            suspect = getString(R.string.crime_report_suspect, suspect);

        String crimeTitle = mCrime.getTitle();
        if (crimeTitle.equals(""))
            crimeTitle = getString(R.string.crime_title_empty);

        return getString(
                R.string.crime_report,
                crimeTitle,
                dateString,
                solvedString,
                suspect);
    }

    private void showPicker(int pickerType) {
        DateTimePickerFragment pickerFragment = DateTimePickerFragment
                .newInstance(mCrime.getDate(), pickerType);
        pickerFragment.setTargetFragment(CrimeFragment.this, CrimeConstants.REQUEST_CODE_PICKER);
        pickerFragment.show(getActivity().getSupportFragmentManager(), CrimeConstants.TAG_DIALOG);
    }

    private void updateDate() {
        String formattedDate = CrimeUtils.formatDate(mCrime.getDate());
        mDateButton.setText(formattedDate);
    }

    private void updateTime() {
        String formattedDate = CrimeUtils.formatTime(getActivity(), mCrime.getDate());
        mTimeButton.setText(formattedDate);
    }
}