package com.choliy.igor.criminalintent.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.choliy.igor.criminalintent.Crime;
import com.choliy.igor.criminalintent.CrimeConstants;
import com.choliy.igor.criminalintent.R;
import com.choliy.igor.criminalintent.data.CrimeLab;
import com.choliy.igor.criminalintent.fragment.CrimeFragment;
import com.choliy.igor.criminalintent.util.InfoUtils;

import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity {

    private List<Crime> mCrimes;
    private int mCrimePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        mCrimes = CrimeLab.getInstance(this).getCrimes();
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);
                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }
        };
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCrimePosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimeConstants.EXTRA_CRIME_ID);
        mCrimePosition = getCrimeIndex(crimeId);
        viewPager.setCurrentItem(mCrimePosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crime, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuDeleteCrime:
                UUID crimeId = mCrimes.get(mCrimePosition).getId();
                InfoUtils.deleteDialog(CrimePagerActivity.this, crimeId, this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int getCrimeIndex(UUID crimeId) {
        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(crimeId)) return i;
        }
        return 0;
    }
}