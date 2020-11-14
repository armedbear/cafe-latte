package systems.raptor.cafe_latte.handlers;

import systems.raptor.cafe_latte.conditions.Condition;
import systems.raptor.cafe_latte.control_flow.block.Block;
import systems.raptor.cafe_latte.control_flow.tagbody.Tagbody;
import systems.raptor.cafe_latte.control_flow.tagbody.TagbodyElement;
import systems.raptor.cafe_latte.control_flow.tagbody.TagbodyTag;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static systems.raptor.cafe_latte.control_flow.block.Block.returnFrom;
import static systems.raptor.cafe_latte.control_flow.tagbody.Tagbody.go;
import static systems.raptor.cafe_latte.control_flow.tagbody.Tagbody.tag;

public class HandlerCase<T> implements Supplier<T> {

  private final Supplier<T> body;
  private final List<Handler<T>> handlers;
  private final Block<T> block;

  public HandlerCase(List<Handler<T>> handlers, Supplier<T> body) {
    this.handlers = handlers;
    this.body = body;
    block = generateBlock();
  }

  static class ConditionStorage {
    Condition transferredCondition;
  }

  private Block<T> generateBlock () {
    ConditionStorage conditionStorage = new ConditionStorage();
    Block<T> block = new Block<>();
    Tagbody tagbody = generateTagbody(block, conditionStorage);
    block.setFunction((block1) -> {
      tagbody.accept(tagbody);
      return null;
    });
    return block;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Tagbody generateTagbody(Block<T> block, ConditionStorage conditionStorage) {
    List<TagbodyElement> tagbodyElements = new LinkedList<>();
    List<Handler<Void>> trampolineHandlers = new LinkedList<>();
    Tagbody tagbody = new Tagbody();
    for (Handler<T> handler : handlers) {
      TagbodyTag tag = tag();
      trampolineHandlers.add(new Handler<>(handler.getConditionClass(), (condition) -> {
        conditionStorage.transferredCondition = condition;
        go(tagbody, tag);
        return null;
      }));
      tagbodyElements.add(tag);
      tagbodyElements.add((tagbody1) ->
              returnFrom(block, handler.apply(conditionStorage.transferredCondition)));
    }
    tagbodyElements.add(0, (tagbody1) -> new HandlerBind(trampolineHandlers, () -> {
      returnFrom(block, body.get());
      return null;
    }).get());
    tagbody.setElements(tagbodyElements.toArray(new TagbodyElement[]{}));
    return tagbody;
  }

  @Override
  public T get() {
    return block.get();
  }
}