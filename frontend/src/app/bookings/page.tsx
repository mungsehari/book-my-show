import { MyBookings } from "@/modules/booking/components/my-bookings";

export default function BookingsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">My bookings</h1>
      <MyBookings />
    </div>
  );
}
