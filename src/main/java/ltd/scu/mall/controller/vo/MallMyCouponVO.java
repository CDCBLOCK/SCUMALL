package ltd.scu.mall.controller.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

public class MallMyCouponVO implements Serializable {

    private static final long serialVersionUID = -8182785776876066101L;

    private Long couponUserId;

    private Long userId;

    private Long couponId;

    private String name;

    private String couponDesc;

    private Integer discount;

    private Integer min;

    private Byte goodsType;

    private String goodsValue;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    public Long getCouponUserId() {
        return couponUserId;
    }

    public MallMyCouponVO setCouponUserId(Long couponUserId) {
        this.couponUserId = couponUserId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public MallMyCouponVO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getCouponId() {
        return couponId;
    }

    public MallMyCouponVO setCouponId(Long couponId) {
        this.couponId = couponId;
        return this;
    }

    public String getName() {
        return name;
    }

    public MallMyCouponVO setName(String name) {
        this.name = name;
        return this;
    }

    public String getCouponDesc() {
        return couponDesc;
    }

    public MallMyCouponVO setCouponDesc(String couponDesc) {
        this.couponDesc = couponDesc;
        return this;
    }

    public Integer getDiscount() {
        return discount;
    }

    public MallMyCouponVO setDiscount(Integer discount) {
        this.discount = discount;
        return this;
    }

    public Integer getMin() {
        return min;
    }

    public MallMyCouponVO setMin(Integer min) {
        this.min = min;
        return this;
    }

    public Byte getGoodsType() {
        return goodsType;
    }

    public MallMyCouponVO setGoodsType(Byte goodsType) {
        this.goodsType = goodsType;
        return this;
    }

    public String getGoodsValue() {
        return goodsValue;
    }

    public MallMyCouponVO setGoodsValue(String goodsValue) {
        this.goodsValue = goodsValue;
        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public MallMyCouponVO setStartTime(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public MallMyCouponVO setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }
}
