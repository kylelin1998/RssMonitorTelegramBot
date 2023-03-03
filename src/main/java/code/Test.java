package code;

import code.config.Config;
import code.config.RequestProxyConfig;
import code.util.TelegraphUtil;
import com.alibaba.fastjson2.JSON;
import kong.unirest.Unirest;
import org.apache.commons.lang3.RandomUtils;

import static code.Main.GlobalConfig;

public class Test {
    public static void main(String[] args) {
        Unirest
                .config()
                .verifySsl(false)
                .enableCookieManagement(false);

        GlobalConfig = Config.readConfig();
        RequestProxyConfig proxyConfig = RequestProxyConfig.create();

        for (int i = 0; i < 30; i++) {
            System.out.println(RandomUtils.nextInt(1, 999));
        }

        String title = "苏流西 我正式宣布【P图技术修炼大赛】开幕！[团圆兔]参赛规则：3月31日前在超话以【参加P图技术修炼大赛】为开头分享原图及你P图后的作品，形式不限，鼓励创新...";
//        System.out.println(StrUtil.symbolConvertToEn(title));
        String content = "<a href=\"https://m.weibo.cn/p/index?extparam=%E8%8B%8F%E6%B5%81%E8%A5%BF&amp;containerid=10080878f467774e2948b024acb35061a25a6d\" data-hide=\"\"><span class=\"url-icon\"><img style=\"width: 1rem;height: 1rem\" src=\"https://n.sinaimg.cn/photo/5213b46e/20180926/timeline_card_small_super_default.png\" referrerpolicy=\"no-referrer\"></span><span class=\"surl-text\">苏流西</span></a> 我正式宣布【P图技术修炼大赛】开幕！<br>[团圆兔]参赛规则：3月31日前在超话以【参加P图技术修炼大赛】为开头分享原图及你P图后的作品，形式不限，鼓励创新。<br>\uD83C\uDF6B获奖规则：<br>1、评委：苏次次<br>2、时间：4月1日将公布获奖者3名<br>3、奖品：任意视频app一个月会员<span class=\"url-icon\"><img alt=\"[憧憬]\" src=\"https://h5.sinaimg.cn/m/emoticon/icon/default/d_xingxingyan-c64b6a744b.png\" style=\"width:1em; height:1em;\" referrerpolicy=\"no-referrer\"></span><br>4、不可盗用他人的作品<img style=\"\" src=\"https://wx3.sinaimg.cn/large/bf624dd1gy1hbk29xldvxj20k00k0mxv.jpg\" referrerpolicy=\"no-referrer\"><br><br>";
        TelegraphUtil.SaveResponse save = TelegraphUtil.save(proxyConfig, title, "2", content, null);
        System.out.println(JSON.toJSONString(save));
    }
}
