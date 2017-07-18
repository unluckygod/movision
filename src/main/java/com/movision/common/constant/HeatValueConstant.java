package com.movision.common.constant;

/**
 * @Author zhanglei
 * @Date 2017/7/11 10:46
 */
public class HeatValueConstant {

    /**
     * 获得热度的类型
     */
    public enum HEATVALUE_TYPE {
        //帖子热度
        home_page_selection(1),//首页精选
        post_selection(2),//帖子精选
        zan_number(3),//点赞数
        comments_number(4),//评论数
        forwarding_number(5),//转发数
        collection_number(6),//收藏数

        //用户热度
        fan_count(7),//粉丝数
        posts_count(8),//发帖数
        //评论的热度
        comment_zan_count(9),//評論的點贊數
        reply_comment_number(10);//回复评论数

        public final int code;

        HEATVALUE_TYPE(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        @Override
        public String toString() {
            return String.valueOf(this.code);
        }
    }

    /**
     * 热度类型对应的积分
     */
    public enum POINT {
        home_page_selection(30),//首页精选
        post_selection(20),//帖子精选
        zan_number(5),//点赞数
        comments_number(5),//评论数
        forwarding_number(5),//转发数
        collection_number(5),//收藏数

        //用户热度
        fan_count(10),//粉丝数
        posts_count(8),//发帖数
        //评论的热度
        comment_zan_count(3),//評論的點贊數
        reply_comment_number(7);//回复评论数
        public final int code;

        POINT(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        @Override
        public String toString() {
            return String.valueOf(this.code);
        }
    }

    public static void main(String[] args) {
        System.out.println(HEATVALUE_TYPE.comments_number.getCode() == 5);
    }


}