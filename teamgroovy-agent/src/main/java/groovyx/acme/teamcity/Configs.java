/**/
package groovyx.acme.teamcity;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

public class Configs{

    public static Map<String,Object> propsToTreeMap(Map<String,String> p, String sfilter){
    	Pattern filter = sfilter==null ? null : Pattern.compile(sfilter);
        Map<String,Object> root=new ConcurrentSkipListMap();
        for( Map.Entry<String,String> it : p.entrySet() ) {
            if(filter==null || filter.matcher(it.getKey()).matches()){
                String [] keys=it.getKey().split("\\.");
                Map<String,Object> cur=root;
                for(int i=0; i<keys.length-1; i++) {
                    if( !(cur.get(keys[i]) instanceof Map) ){
                        cur.put(keys[i], new ConcurrentSkipListMap());
                    }
                    cur = (Map<String,Object>)cur.get(keys[i]);
                }
                if( ! (cur.get(keys[keys.length-1]) instanceof Map) ){
                    cur.put(keys[keys.length-1],it.getValue());
                }
            }
        }
        return root;
    }

}
