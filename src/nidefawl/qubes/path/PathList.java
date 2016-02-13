package nidefawl.qubes.path;

import java.util.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.perf.TimingHelper;

public class PathList {
    LinkedList<PathPoint> list = new LinkedList<>();
    private Comparator<PathPoint> comparator = new Comparator<PathPoint>() {
        
        @Override
        public int compare(PathPoint o1, PathPoint o2) {
            return 0;
        }
    };
    
    List<PathPoint> points = Lists.newArrayList();
    public void reset() {
        this.list.clear();
    }
    public void addPoint(PathPoint start) {
//        TimingHelper.start(5);
        int insertionPoint = Collections.binarySearch(list, start, comparator);
        insertionPoint = (insertionPoint > -1) ? insertionPoint : (-insertionPoint) - 1;
        list.add(insertionPoint, start);
        int pos = 0;
        for (PathPoint p : list) {
            p.pos = pos++;
        }
//        TimingHelper.end(5);
    }
 
    public boolean containsElement(PathPoint start) {
        return (Collections.binarySearch(list, start, comparator) > -1);
    }
    public boolean isEmpty() {
        return list.isEmpty();
    }
    public PathPoint pop() {
        PathPoint p = list.remove(0);
        p.pos = -1;
        return p;
    }
    public void updateCost(PathPoint point2, float f) {
        point2.distanceToTarget = f;
//        TimingHelper.start(6);
        Collections.sort(list, this.comparator);
//        TimingHelper.end(6);
    }
    

}
