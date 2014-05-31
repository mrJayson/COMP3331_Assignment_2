import java.io.Serializable;



public interface Message extends Sendable{
	
	public void execute(Graph g);

}
