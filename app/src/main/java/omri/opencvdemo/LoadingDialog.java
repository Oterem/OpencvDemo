package omri.opencvdemo;

import android.support.v7.app.AppCompatActivity;
import android.app.ProgressDialog;

/**
 * Created by oterem on 26/05/2018.
 */

public class LoadingDialog extends AppCompatActivity{
    public ProgressDialog mProgressDialog;

    public void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(message);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }


}
