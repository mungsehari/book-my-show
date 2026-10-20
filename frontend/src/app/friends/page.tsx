import { AddFriendForm } from "@/modules/friend/components/add-friend-form";
import { FriendsList } from "@/modules/friend/components/friends-list";
import { PrivacyToggle } from "@/modules/friend/components/privacy-toggle";
import { RequestsList } from "@/modules/friend/components/requests-list";

export default function FriendsPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-8">
      <h1 className="text-2xl font-bold tracking-tight">Friends</h1>
      <AddFriendForm />
      <RequestsList />
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Your friends</h2>
        <FriendsList />
      </section>
      <PrivacyToggle />
    </div>
  );
}
