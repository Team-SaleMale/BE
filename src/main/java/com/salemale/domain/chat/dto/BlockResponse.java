@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {
    private Long blockedUserId;
    private boolean blocked;  // true → 차단됨, false → 차단 해제됨
}
