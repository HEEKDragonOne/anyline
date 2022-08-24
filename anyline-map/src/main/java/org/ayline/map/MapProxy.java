package org.ayline.map;

import org.anyline.amap.util.AmapUtil;
import org.anyline.baidu.map.util.BaiduMapUtil;
import org.anyline.entity.MapPoint;
import org.anyline.exception.AnylineException;
import org.anyline.qq.map.util.QQMapUtil;
import org.anyline.util.BasicUtil;
import org.anyline.util.DateUtil;
import org.anyline.util.GISUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MapProxy {
    private static AmapUtil amap;
    private static QQMapUtil qmap;
    private static BaiduMapUtil bmap;
    public static Map<String,String> over_limits = new HashMap<>();
    public static MapPoint regeo(GISUtil.COORD_TYPE coord, String lng, String lat){
        return regeo(coord, BasicUtil.parseDouble(lng, 0d), BasicUtil.parseDouble(lat, 0d));
    }
    private static boolean enable(String type, String platform){
        String ymd = over_limits.get(type+"_"+platform);
        if(null == ymd){
            return true;
        }
        if(DateUtil.format("yyyy-MM-dd").equals(ymd)){
            return false;
        }
        over_limits.remove(type+"_"+platform);
        return true;
    }

    /**
     *
     * @param coord 坐标系
     * @param lng 经度
     * @param lat 纬度
     * @return MapPoint
     */
    public static MapPoint regeo(GISUtil.COORD_TYPE coord, double lng, double lat){
        MapPoint point = null;
        String type = "regeo";
        double[] location = null;
        if(null != amap && enable(type, "amap")){
            try{
                location = GISUtil.convert(coord, lng, lat, GISUtil.COORD_TYPE.GCJ02LL);
                point = amap.regeo(location[0], location[1]);
            }catch (AnylineException e){
                if("API_OVER_LIMIT".equals(e.getCode())){
                    over_limits.put(type+"_amap", DateUtil.format("yyyy-MM-dd"));
                }
            }
        }
        if(null == point && null != bmap && enable(type,"bmap")){
            try{
                location = GISUtil.convert(coord, lng, lat, GISUtil.COORD_TYPE.BD09LL);
                point = bmap.regeo(location[0], location[1]);
            }catch (AnylineException e){
                if("API_OVER_LIMIT".equals(e.getCode())){
                    over_limits.put(type+"_bmap", DateUtil.format("yyyy-MM-dd"));
                }
            }
        }
        if(null == point && null != qmap && enable(type,"qmap")){
            try{
                location = GISUtil.convert(coord, lng, lat, GISUtil.COORD_TYPE.GCJ02LL);
                point = qmap.regeo(location[0], location[1]);
            }catch (AnylineException e){
                if("API_OVER_LIMIT".equals(e.getCode())){
                    over_limits.put(type+"_qmap", DateUtil.format("yyyy-MM-dd"));
                }
            }
        }
        return point;
    }

    public static AmapUtil getAmap() {
        return MapProxy.amap;
    }

    public static void setAmap(AmapUtil amap) {
        MapProxy.amap = amap;
    }

    public static QQMapUtil getQmap() {
        return MapProxy.qmap;
    }

    public static void setQmap(QQMapUtil qmap) {
        MapProxy.qmap = qmap;
    }

    public static BaiduMapUtil getBmap() {
        return MapProxy.bmap;
    }

    public static void setBmap(BaiduMapUtil bmap) {
        MapProxy.bmap = bmap;
    }


    @Autowired(required = false)
    public void init(AmapUtil amap){
        MapProxy.amap = amap;
    }
    @Autowired(required = false)
    public void init(QQMapUtil qmap){
        MapProxy.qmap = qmap;
    }
    @Autowired(required = false)
    public void init(BaiduMapUtil bmap){
        MapProxy.bmap = bmap;
    }
}
