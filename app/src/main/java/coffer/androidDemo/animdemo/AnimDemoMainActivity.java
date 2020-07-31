package coffer.animDemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import coffer.androidDemo.animdemo.FrameAnimActivity;
import coffer.androidDemo.animdemo.AnimTestActivity;
import coffer.androidDemo.animdemo.PropertyAnimActivity;
import coffer.androidDemo.animdemo.TweenAnimActivity;
import coffer.androidjatpack.R;

/**
 * @author：张宝全
 * @date：2020/5/2
 * @Description： 动画练习
 * @Reviser：
 * @RevisionTime：
 * @RevisionDescription：
 */
public class AnimDemoMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim_main);

        // 属性动画
        findViewById(R.id.b1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnimDemoMainActivity.this, PropertyAnimActivity.class);
                startActivity(intent);
            }
        });

        // 逐帧动画
        findViewById(R.id.b2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnimDemoMainActivity.this, FrameAnimActivity.class);
                startActivity(intent);
            }
        });

        // 补间动画
        findViewById(R.id.b3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnimDemoMainActivity.this, TweenAnimActivity.class);
                startActivity(intent);
            }
        });

        // 动画demo
        findViewById(R.id.b3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnimDemoMainActivity.this, AnimTestActivity.class);
                startActivity(intent);
            }
        });

    }
}
