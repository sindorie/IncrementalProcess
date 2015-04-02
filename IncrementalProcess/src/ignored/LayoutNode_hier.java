package ignored;

import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import com.android.hierarchyviewerlib.models.ViewNode;
 
public class LayoutNode_hier extends DefaultMutableTreeNode{

	private static final long serialVersionUID = 1L;
	public LayoutNode_hier parent;
	
	public int baseline, index, height, width, left, top,
			   marginLeft, marginRight, marginTop, marginBottom,
			   paddingBottom, paddingTop, paddingRight, paddingLeft,
			   scrollX, scrollY ;
	
	public int rTop = -1, rLeft = -1;
	public String hashCode, id, name;
	public Set<String> categories;
	public boolean filter,hashFocus,hasMargins, willNotDraw;	
	
	public LayoutNode_hier(ViewNode node){
		this(node, null);
	}
	
	public LayoutNode_hier(ViewNode node, LayoutNode_hier parent){
		this.baseline = node.baseline;
		this.categories = node.categories;
		this.filter = node.filtered;
		this.hashFocus = node.hasFocus;
		this.hashCode = node.hashCode;
		this.hasMargins = node.hasMargins;
		this.id = node.id;
		this.height = node.height;
		this.width = node.width;
		this.index = node.index;
		this.left = node.left;
		this.top = node.top;
		this.name = node.name;
		this.marginBottom = node.marginBottom;
		this.marginLeft = node.marginLeft;
		this.marginRight = node.marginRight;
		this.marginTop = node.marginTop;
		this.paddingBottom = node.paddingBottom;
		this.paddingLeft = node.paddingLeft;
		this.paddingRight = node.paddingRight;
		this.paddingTop = node.paddingTop;
		this.scrollX = node.scrollX;
		this.scrollY = node.scrollY;
		this.willNotDraw = node.willNotDraw;

		if(parent != null){
			this.rTop  = this.top + parent.rTop;
			this.rLeft = this.left + parent.rLeft;
		}else{
			this.rTop = this.top;
			this.rLeft = this.left;
		}
		
		for(ViewNode child: node.children){
			this.add(new LayoutNode_hier(child, this));
		}
		
//		node.namedProperties;
//		node.parent;
//		node.properties;
//		node.protocolVersion;
//		node.referenceImage();
//		node.viewCount; 
//		node.window;
//		node.layoutTime;
	}
	
	@Override
	public String toString(){
		return this.name+": "+this.rLeft+","+this.rTop+" | "+this.left+","+this.top;
	}
	
	public void add(LayoutNode_hier newChild){
		super.add(newChild); 
	}
	
	public LayoutNode_hier getChildAt(int index){
		return (LayoutNode_hier) super.getChildAt(index);
	}

}
