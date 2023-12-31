import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class DrawerView extends JPanel      //이 판넬은 프레임에서 setWhatToDraw함수로 뭐그릴지 받아와서 해당 객체를 그림
        implements MouseListener, MouseMotionListener{      //JDrawer같은 큰 프로젝트의 경우 함수를 다 사용할 여지가 있음

    /*                                                        //1번방식처럼 JPanel에다가 직접 구현해줌
    int startX;
    int startY;
    int oldX;       //rubber banding에 필요 (마지막 좌표값 기억)
    int oldY;

     */
    public static String[] figureType = {"Point", "Box", "Line", "Circle", "TV", "Kite"};

    public static int INIT_WIDTH = 3000;
    public static int INIT_HEIGHT = 1500;

    public static int ID_POINT = 0;
    public static int ID_BOX = 1;
    public static int ID_LINE = 2;
    public static int ID_CIRCLE = 3;
    public static int ID_TV = 4;
    public static int ID_KITE = 5;

    public static int NOTHING = 0;
    public static int DRAWING = 1;
    public static int MOVING = 2;


    private int actionMode;     //마우스 DRAG 할때 그리는 모드인지, MOVE하는 모드인지
    public static int whatToDraw;   //프레임객체에서 setWhatToDraw함수로 뭐그릴지 받아온 값을 저장
    /*private Box pBox;
    private Box[] boxes = new Box[MAX];
    private int nBox = 0; // 배열 대신 ArrayList 사용
    private ArrayList<Box> boxes = new ArrayList<Box>();
    private Line pLine;
    private ArrayList<Line> lines = new ArrayList<Line>();*/    //상속을 이용해 전부 Figure라는 객체로 처리
    private Figure selectedFigure;   //현재 작업중인 figure
    private Color selectedColor;
    private ArrayList<Figure> figures = new ArrayList<Figure>();    //polymorphic container 다양한 걸 담는다

    private int currentX;   //마우스 drag할때 좌표값저장
    private int currentY;

    private Popup mainPopup;    //빈공간 우클릭
    private Popup pointPopup;
    private Popup boxPopup;     //도형 우클릭
    private Popup linePopup;
    private Popup circlePopup;
    private Popup tvPopup;
    private Popup kitePopup;

    private SelectAction pointAction;
    private SelectAction boxAction;
    private SelectAction lineAction;
    private SelectAction circleAction;
    private SelectAction TVAction;
    private SelectAction kiteAction;


    private DrawerFrame mainFrame;

    private double zoomRatio = 1.0;

    private int width = INIT_WIDTH;
    private int height = INIT_HEIGHT;

    DrawerView(DrawerFrame mainFrame){
        this.mainFrame = mainFrame;

        pointAction = new SelectAction("Point (P)", new ImageIcon("box.gif"), this, ID_POINT);
        boxAction = new SelectAction("Box (B)", new ImageIcon("box.gif"), this, ID_BOX);
        lineAction = new SelectAction("Line (L)", new ImageIcon("box.gif"), this, ID_LINE);
        circleAction = new SelectAction("Circle (C)", new ImageIcon("box.gif"), this, ID_CIRCLE);
        TVAction = new SelectAction("TV (T)", new ImageIcon("box.gif"), this, ID_TV);
        kiteAction = new SelectAction("Kite (K)", new ImageIcon("box.gif"), this, ID_KITE);

        mainPopup = new MainPopup(this);
        boxPopup = new FigurePopup(this, "Box", true);   //이벤트핸들링을위해 this넘겨줌
        linePopup = new FigurePopup(this, "Line", false);    //Line은 채우기메뉴 false
        circlePopup = new FigurePopup(this, "Circle", true);
        pointPopup = new FigurePopup(this, "Point", false);
        tvPopup = new TVPopup(this);
        kitePopup = new FigurePopup(this, "Kite", true);


        actionMode = NOTHING;
        setWhatToDraw(ID_BOX);
        addMouseListener(this);     //등록
        addMouseMotionListener(this);    //등록

        setPreferredSize(new Dimension(width,height)); //스크롤바 생성을 위한 Panel의 논리적크기 명시
    }
    public ArrayList<Figure> getFigures(){
        return figures;
    }
    SelectAction getPointAction(){
        return pointAction;
    }
    SelectAction getBoxAction(){
        return boxAction;
    }
    SelectAction getLineAction(){
        return lineAction;
    }
    SelectAction getCircleAction(){
        return circleAction;
    }
    SelectAction getTVAction(){
        return TVAction;
    }
    SelectAction getKiteAction(){
        return kiteAction;
    }
    Popup pointPopup(){
        return pointPopup;
    }
    Popup boxPopup(){
        return boxPopup;
    }
    Popup linePopup(){
        return linePopup;
    }
    Popup circlePopup(){ return circlePopup; }
    Popup tvPopup(){ return tvPopup; }
    Popup kitePopup(){ return kitePopup; }

    void setWhatToDraw(int id) {
        whatToDraw = id;
        mainFrame.writeFigureType(figureType[id]);
    }

    public void doOpen(String fileName){   // 파일 Load해서 open하기
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            //자료구조에 load
            figures = (ArrayList<Figure>) (ois.readObject()); //readObject의 return type은 default로 Object -> typeCasting
            ois.close();
            fis.close();
            repaint();
        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
        }
    }
    public void doSave(String fileName){   // 파일 save하기
        try {
            FileOutputStream fos = new FileOutputStream(fileName);  // 1.파일이름으로부터 파일 outputStream만들기
            ObjectOutputStream oos = new ObjectOutputStream(fos);   //2. fileOutputStreame을 ObjectOutputStream으로 변환
            oos.writeObject(figures);  // 가장 핵심적인 정보 저장 ( 이 app에서는 FigureList figures)
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException ex) {  //예외처리해줘야함
        }
    }

    //paint event (화면이 나타나거나 갱신될때마다 호출되는 핸들러)
    public void paintComponent(Graphics g) {    //많이쓰이는 이벤트핸들러라서 인터페이스로 존재하지않고 상위클래스에 있음
        super.paintComponent(g);    //paintComponent함수 이용할때 반드시 써줘야함

        ((Graphics2D)g).scale(zoomRatio,zoomRatio);
        /*Box[] arr = (Box[])boxes.toArray(); //ArrayList를 array로 바꾸기
        for (int i = 0; i < arr.length; i++) {  // Collection객체 boxes에 저장된 box만큼 반복해서 draw()실행
            arr[i].draw(g);
        }*/ //아래는 foreach 문법을 이용하는 방법
        for (Figure pFigure : figures) {    // : 오른쪽엔 collection객체, 모든 객체들 travels
            pFigure.draw(g);    //이게 Dynamic Binding : reference의 type에 따라 알아서 draw가 호출됨
        }

    }
    public void zoom(int ratio){    //툴바 줌기능
        zoomRatio = (double) ratio / 100.0;
        repaint();
        removeMouseListener(this    );      //zoom기능으로 화면비율 변경시 이벤트핸들링 불가조치
        removeMouseMotionListener(this    );
        if (ratio == 100) {
            addMouseListener(this);
            addMouseMotionListener(this    );
        }
    }
    public void mouseClicked(MouseEvent e){    }
    public void mousePressed(MouseEvent e) {// 빈화면에서 눌렀을때: 그림그리기, 그림위에서 눌렀을때 move
        if(zoomRatio != 1.0) return;
        int x = e.getX();
        int y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON3) {  //우클릭은 버튼3
            actionMode = NOTHING;
            return;
        }
        selectedFigure = find(x, y); //figure객체 위에 마우스가 올라와있는가, 찾으면 해당객체, 못찾으면 null return
        if (selectedFigure != null) {    //마우스 커서가ㅏ figure객체를 찾은경우
            actionMode = MOVING;
            currentX = x;   //Figure move하기전 이동시작위치 저장
            currentY = y;
            figures.remove(selectedFigure);  //움직인 자리에 새로 그린걸 생성해주므로 현재위치의 figure는 collection에서 제거
            repaint();
            return;
        }
        if(whatToDraw == ID_POINT){
            selectedFigure = new Point(Color.BLACK, x, y);
            selectedFigure.setPopup(pointPopup);
        } else if (whatToDraw == ID_BOX) {   //프레임객체에서 setWhatToDraw함수로 받아와 저장해놓은 whatToDraw에 따라 그릴 그림결정
            selectedFigure = new Box(Color.BLACK, x, y);
            selectedFigure.setPopup(boxPopup);
        } else if (whatToDraw == ID_LINE) {
            selectedFigure = new Line(Color.BLACK, x, y);
            selectedFigure.setPopup(linePopup);
        } else if (whatToDraw == ID_CIRCLE) {
            selectedFigure = new Circle(Color.BLACK, x, y);
            selectedFigure.setPopup(circlePopup);
        } else if (whatToDraw == ID_TV) {
            selectedFigure = new TV(Color.BLACK, x, y, true);
            selectedFigure.setPopup(tvPopup);
            addFigure(selectedFigure);
            selectedFigure = null;
            actionMode = NOTHING;
            return;
        } else if (whatToDraw == ID_KITE) {
            selectedFigure = new Kite(Color.BLACK, x, y);
            selectedFigure.setPopup(kitePopup);
            addFigure(selectedFigure);
            return;
        }
        actionMode = DRAWING;
    }
    public void mouseReleased(MouseEvent e){    //Dragged와 마찬가지로 그리는모드랑 move모드 나눠서 실행
        int x = e.getX();
        int y = e.getY();

        if (e.isPopupTrigger()) {   //마우스 우클릭(popup기능)시 인가
            selectedFigure = find(x, y);
            if (selectedFigure == null) {
                mainPopup.popup(this, x, y);
            } else{
                selectedFigure.popup(this, x, y);
            }

            //1. 기본 방법
            /*JPopupMenu popupPtr;    //팝업메뉴창 생성
            popupPtr = new JPopupMenu("그림");

            popupPtr.add("그림");     //글자Item
            popupPtr.addSeparator();

            JMenuItem boxItem = new JMenuItem("Box (B)");   //기능Item
            popupPtr.add(boxItem);
            boxItem.addActionListener((evt) ->{
                setWhatToDraw(DrawerView.DRAW_BOX);
            });

            JMenuItem lineItem = new JMenuItem("Line (L)");
            popupPtr.add(lineItem);
            lineItem.addActionListener((evt) ->{
                setWhatToDraw(DrawerView.DRAW_LINE);
            });

            popupPtr.show(this, x, y);*/

            //2. Figure객체들이 공유하는 Popup클래스 이용하는 방법
            //MainPopup popup = new MainPopup(this); -> 매번 새로 mainpopup을 만들지 않고 데이터멤버화해서 사용
            mainPopup.popup(this,x, y);  //Mainpopup객체 popup의 메서드인 popup실행 _우클릭 popup창 보이기
            //argument : popup창을 띄울 판넬 this, 띄울 마우스좌표 x, y

            return;
        }
        if(selectedFigure == null) return;
        Graphics g = getGraphics();
        if (actionMode == DRAWING) {
             /* 함수화
        pBox.setX2(e.getX());
        pBox.setY2(e.getY());*/
            selectedFigure.setXY2(x, y);   //figure 객체의 x2, y2 access function(입력)
        }
         /*
            int minX = Math.min(boxes[i].x1, boxes[i].x2);
            int minY = Math.min(boxes[i].y1, boxes[i].y2);
            int width = Math.abs(boxes[i].x2 - boxes[i].x1);
            int height = Math.abs(boxes[i].y2 - boxes[i].y1);

            g.drawRect(minX, minY, width, height);
            여기까지 Box 그리는 행위를 Box의 Member Function으로 만들어주기
        */
        selectedFigure.draw(g);
        selectedFigure.makeRegion();
        figures.add(selectedFigure); //만든 Figure Collection객체에 넣기,   polymorphic collection객체
        selectedFigure = null;   //만든 객체 figures에 넣어줬으니까 현재 작업중인 selectedFigure은 비워주기
    }
    public void remove(Figure ptr){
        figures.remove(ptr);
        repaint();
    }
    public void remove(int index){
        if(index < 0) return;
        figures.remove(index);
        repaint();
    }
    public void addFigure(Figure newFigure){    //dialog에서 만든 Figure 추가하는 함수
        newFigure.makeRegion();
        figures.add(newFigure);
        repaint();  //paint component함수 다시 실행해줌
    }
    public void copyFigure(){
        if (selectedFigure == null) {
            return;
        } else{
            Figure newFigure = selectedFigure.copy();
            addFigure(newFigure);
            selectedFigure = newFigure;
            repaint();
        }

    }
    public void deleteFigure(){
        if (selectedFigure == null) {
            return;
        } else{
            figures.remove(selectedFigure);
            selectedFigure = null;
            repaint();
        }

    }
    public void fillFigure(){
        if (selectedFigure == null) {
            return;
        }
/*
        if (selectedFigure instanceof Box) {    //RTTI
            Box pBox = (Box) selectedFigure;
            pBox.setFill();
        }
*/
        selectedFigure.setFill();
        repaint();
    }
    void setColorForSelectedFigure(Color color){
        if(selectedFigure.color == null) return;
        selectedFigure.setColor(color);
        repaint();
    }
    public void showColorChooser(){
        Color color = JColorChooser.showDialog(null, "Color Chooser", Color.black);
        setColorForSelectedFigure(color);
    }
    public void onOffTV(){
        if(selectedFigure == null) return;
        if (selectedFigure instanceof TV) {
            TV tv = (TV)selectedFigure;
            tv.pressPowerButton();
            repaint();
        }
    }
    public void setAntenna(){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mouseDragged(MouseEvent e) {    //마우스 drag할떼 그림그리는경우, figure 옮기는 경우
        int x = e.getX();
        int y = e.getY();
        Graphics g = getGraphics();
        g.setXORMode(getBackground());
        if(actionMode == DRAWING){
            /*
        pBox.draw(g);   // x1,y1과 움직이기 직전의 x2,y2로 Background Color로 그리기(rubber banding)
        pBox.setXY2(e.getX(), e.getY());    //마우스 움직이자마자 x2,y2에 마우스의 새 좌표값 받아오기
        pBox.draw(g);   //저장돼있는 x1,y1과 새로받은 x2,y2로 그리기
        를 함수로 만들어 사용*/
            selectedFigure.drawing(g, x, y); //rubber banding 해서 그리는 함수
        } else if (actionMode == MOVING) {
            selectedFigure.move(g, x - currentX, y - currentY);
            currentX = x;
            currentY = y;
        }


        /*      rubber banding 원시적인 방법
        int minX = Math.min(startX, oldX);
        int minY = Math.min(startY, oldY);
        int width = Math.abs(oldX - startX);
        int height = Math.abs(oldY - startY);
        Graphics g = getGraphics();
        g.setColor(getBackground());
        g.drawRect(minX,minY,width,height);

        int endX = e.getX();
        int endY = e.getY();

        minX = Math.min(startX, endX);
        minY = Math.min(startY, endY);
        width = Math.abs(endX - startX);
        height = Math.abs(endY - startY);
        g.setColor(Color.black);
        g.drawRect(minX , minY, width, height);

        oldX = endX;
        oldY = endY;
*/
    }
    //figures 컬렉션객체를 전부 돌면서 그 객체의 polygon타입 region을 확인해서 마우스의 좌표(x,y)가 그 안에 있으면 그 figure객체를 return
    //못찾으면 null return;
    private Figure find(int x, int y){
        for (Figure pFigure : figures) {
            if (pFigure.contains(x,y)) {
                return pFigure;
            }
        }
        return null;
    }
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        selectedFigure = find(x, y);     //커서가 Figure객체 위에 올라갓는지 판단
        if (selectedFigure != null) {    //올라갔다면
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }else{
            setCursor(Cursor.getDefaultCursor());
        }

        mainFrame.writePosition("[" + x + "," + y + "]");
    }
}
