package com.javachina.controller;

import com.blade.Blade;
import com.blade.ioc.annotation.Inject;
import com.blade.jdbc.core.Take;
import com.blade.jdbc.model.Paginator;
import com.blade.kit.DateKit;
import com.blade.kit.FileKit;
import com.blade.kit.PatternKit;
import com.blade.kit.StringKit;
import com.blade.kit.json.JSONObject;
import com.blade.mvc.annotation.Controller;
import com.blade.mvc.annotation.PathParam;
import com.blade.mvc.annotation.Route;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.Request;
import com.blade.mvc.http.Response;
import com.blade.mvc.multipart.FileItem;
import com.blade.mvc.view.ModelAndView;
import com.javachina.Constant;
import com.javachina.Types;
import com.javachina.dto.HomeTopic;
import com.javachina.kit.FamousDay;
import com.javachina.kit.MapCache;
import com.javachina.kit.SessionKit;
import com.javachina.kit.Utils;
import com.javachina.model.LoginUser;
import com.javachina.model.Node;
import com.javachina.model.NodeTree;
import com.javachina.service.FavoriteService;
import com.javachina.service.NodeService;
import com.javachina.service.NoticeService;
import com.javachina.service.TopicService;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 首页控制器
 */
@Controller
public class IndexController extends BaseController {

	@Inject
	private TopicService topicService;
	
	@Inject
	private NodeService nodeService;
	
	@Inject
	private NoticeService noticeService;
	
	@Inject
	private FavoriteService favoriteService;

	private MapCache mapCache = MapCache.single();

	/**
	 * 首页热门
	 */
	@Route(value = "/", method = HttpMethod.GET)
	public ModelAndView show_home(Request request, Response response){

		this.putData(request);
		
		// 帖子
		String tab = request.query("tab");
		Integer page = request.queryAsInt("p");
		Integer nid = null;
		
		if(StringKit.isNotBlank(tab)){
			Take np = new Take(Node.class);
			np.eq("is_del", 0).eq("slug", tab);
			Node node = nodeService.getNode(np);
			if(null != node){
				nid = node.getNid();
				request.attribute("tab", tab);
				request.attribute("node_name", node.getTitle());
			}
		}
		
		Paginator<HomeTopic> topicPage = topicService.getHomeTopics(nid, page, 20);
		request.attribute("topicPage", topicPage);
		
		// 最热帖子
		List<HomeTopic> hotTopics = mapCache.get(Constant.C_HOT_TOPICS);
		if(null == hotTopics){
			hotTopics = topicService.getHotTopics(1, 10);
			mapCache.set(Constant.C_HOT_TOPICS, hotTopics, 60 * 10);
		}
		request.attribute("hot_topics", hotTopics);

		// 最热门的8个节点
		List<Node> hotNodes = mapCache.get(Constant.C_HOT_NODES);
		if(null == hotNodes){
			hotNodes = nodeService.getHotNodes(1, 8);
			mapCache.set(Constant.C_HOT_NODES, hotNodes, 60 * 60 * 12);
		}
		request.attribute("hot_nodes", hotNodes);
		
		return this.getView("home");
	}
	
	/**
	 * 最新
	 */
	@Route(value = "/recent", method = HttpMethod.GET)
	public ModelAndView show_recent(Request request){
		
		this.putData(request);
		
		// 帖子
		String tab = request.query("tab");
		Integer page = request.queryAsInt("p");
		Integer nid = null;
		
		if(StringKit.isNotBlank(tab)){
			Take np = new Take(Node.class);
			np.eq("is_del", 0).eq("slug", tab);
			Node node = nodeService.getNode(np);
			if(null != node){
				nid = node.getNid();
				request.attribute("tab", tab);
				request.attribute("node_name", node.getTitle());
			}
		}
		
		Paginator<HomeTopic> topicPage = topicService.getRecentTopics(nid, page, 15);
		request.attribute("topicPage", topicPage);

		// 最热帖子
		List<HomeTopic> hotTopics = mapCache.get(Constant.C_HOT_TOPICS);
		if(null == hotTopics){
			hotTopics = topicService.getHotTopics(1, 10);
			mapCache.set(Constant.C_HOT_TOPICS, hotTopics, 60 * 10);
		}
		request.attribute("hot_topics", hotTopics);

		// 最热门的8个节点
		List<Node> hotNodes = mapCache.get(Constant.C_HOT_NODES);
		if(null == hotNodes){
			hotNodes = nodeService.getHotNodes(1, 8);
			mapCache.set(Constant.C_HOT_NODES, hotNodes, 60 * 60 * 12);
		}
		request.attribute("hot_nodes", hotNodes);
		
		return this.getView("recent");
	}

	/**
	 * 放置节点
	 *
	 * @param request
     */
	private void putData(Request request){

		List<NodeTree> nodes = mapCache.get(Constant.C_HOME_NODE_KEY);
		if(null == nodes){
			// 读取节点列表
			nodes = nodeService.getTree();
			mapCache.set(Constant.C_HOME_NODE_KEY, nodes, 60 * 60 * 12);
		}

		request.attribute("nodes", nodes);

		FamousDay famousDay = mapCache.get(Constant.C_HOME_FAMOUS_KEY);
		if(null == famousDay){
			// 每日格言
			famousDay = Utils.getTodayFamous();
			mapCache.set(Constant.C_HOME_FAMOUS_KEY, famousDay, 60 * 60 * 12);
		}

		request.attribute("famousDay", famousDay);
	}
	
	/**
	 * 节点主题页
	 */
	@Route(value = "/go/:slug", method = HttpMethod.GET)
	public ModelAndView go(@PathParam("slug") String slug,
			Request request, Response response){
		
		LoginUser loginUser = SessionKit.getLoginUser();
		Take np = new Take(Node.class);
		np.eq("is_del", 0).eq("slug", slug);
		Node node = nodeService.getNode(np);
		if(null == node){
			// 不存在的节点
//			response.text("not found node.");
			response.go("/");
			return null;
		}
		
		if(null == loginUser){
			SessionKit.setCookie(response, Constant.JC_REFERRER_COOKIE, request.url());
		} else {
			// 查询是否收藏
			boolean is_favorite = favoriteService.isFavorite(Types.node.toString(), loginUser.getUid(), node.getNid());
			request.attribute("is_favorite", is_favorite);
		}
		
		Integer page = request.queryAsInt("page");

		Paginator<HomeTopic> topicPage = topicService.getRecentTopics(node.getNid(), page, 15);
		request.attribute("topicPage", topicPage);
		
		Map<String, Object> nodeMap = nodeService.getNodeDetail(null, node.getNid());
		request.attribute("node", nodeMap);
		
		return this.getView("node_detail");
	}
	
	
	/**
	 * 上传头像
	 */
	@Route(value = "/uploadimg", method = HttpMethod.POST)
	public void uploadimg(Request request, Response response){
		LoginUser user = SessionKit.getLoginUser();
		if(null == user){
			return;
		}
		FileItem[] fileItems = request.files();
		if(null != fileItems && fileItems.length > 0){
			
			FileItem fileItem = fileItems[0];
			
			String type = request.query("type");
			String suffix = FileKit.getExtension(fileItem.fileName());
			if(StringKit.isNotBlank(suffix)){
				suffix = "." + suffix;
			}
			if(!PatternKit.isImage(suffix)){
				return;
			}
			
			if(null == type){
				type = "temp";
			}
			
			String saveName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10) + suffix;
			File file = new File(Blade.$().webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
			
			try {

				Utils.copyFileUsingFileChannels(fileItem.file(), file);
				
				String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
				
				JSONObject res = new JSONObject();
				res.put("status", 200);
				res.put("savekey", filePath);
				res.put("savepath", filePath);
				res.put("url", Constant.SITE_URL + "/" + filePath);
				
				response.json(res.toString());
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * markdown页面
	 */
	@Route(value = "/markdown", method = HttpMethod.GET)
	public ModelAndView markdown(Request request, Response response){
		return this.getView("markdown");
	}
	
	/**
	 * about页面
	 */
	@Route(value = "/about", method = HttpMethod.GET)
	public ModelAndView about(Request request, Response response){
		return this.getView("about");
	}
	
	/**
	 * faq页面
	 */
	@Route(value = "/faq", method = HttpMethod.GET)
	public ModelAndView faq(Request request, Response response){
		return this.getView("faq");
	}
	
	/**
	 * donate页面
	 */
	@Route(value = "/donate", method = HttpMethod.GET)
	public ModelAndView donate(Request request, Response response){
		return this.getView("donate");
	}

}
