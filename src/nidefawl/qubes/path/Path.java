package nidefawl.qubes.path;

public class Path {

    PathPoint[] arr;
    int curPos;

    public Path(PathPoint[] arr) {
        this.arr = arr;
    }
    
    public boolean isFinished() {
        return this.curPos >= this.arr.length;
    }

    public PathPoint get() {
        return this.arr[this.curPos];
    }
    public PathPoint getEnd() {
        return this.arr[this.arr.length-1];
    }
    public void incr() {
        this.curPos++;
        if (this.curPos > this.arr.length-1) {
            this.curPos = this.arr.length-1;
        }
    }

    public int getLength() {
        return this.arr.length;
    }

    public int getPos() {
        return this.curPos;
    }

    public PathPoint get(int i) {
        return i<0||i>=this.arr.length?null:this.arr[i];
    }

    public void setPos(int minIdx) {
        this.curPos = minIdx;
        if (this.curPos > this.arr.length-1) {
            this.curPos = this.arr.length-1;
        }
    }
    
    

}
