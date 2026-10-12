/*
 * Pixel 9 Pro IMS - 最小入口界面
 *
 * 这个界面没有功能，它唯一的用途是让 App 能被"启动"一次。
 * Android 会把刚安装、从未启动过的 App 标记为 stopped 状态，
 * 而 LSPosed 不会把处于 stopped 状态的模块注入目标进程。
 * 启动一次即可清除该标记。
 */
package dev.greydust.ims;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setPadding(56, 120, 56, 56);
        tv.setTextSize(15f);
        tv.setText(
            "Pixel 9 Pro IMS\n\n" +
            "VoNR / carrier config 覆写模块，无设置界面。\n\n" +
            "这个界面没有实际功能——它存在的唯一原因是让 App 能被启动一次。\n\n" +
            "Android 会把刚装好、从未启动过的 App 标记为 stopped，\n" +
            "而 LSPosed 不会向目标进程注入处于 stopped 状态的模块。\n" +
            "打开本界面一次即可清除该标记，之后重启手机模块才会生效。\n\n" +
            "作用域：com.android.phone"
        );
        setContentView(tv);
    }
}
